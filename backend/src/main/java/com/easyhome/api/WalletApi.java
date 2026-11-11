package com.easyhome.api;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class WalletApi {
  private final WalletService wallets;private final WalletEntries entries;private final Identity identity;
  private final Withdrawals withdrawals;private final Audits audits;private final Environment env;
  private final boolean mock;
  WalletApi(WalletService w,WalletEntries e,Identity i,Withdrawals req,Audits a,Environment env,
      @Value("${app.mock-payments:false}") boolean mock){wallets=w;entries=e;identity=i;withdrawals=req;audits=a;this.env=env;this.mock=mock;}
  public record Balance(long demoAvailable,long demoHeld,String warning){}
  public record LedgerView(long delta,String action,String reference,Instant at){}
  public record Amount(long amount){}
  public record WithdrawalInput(long amount,String bankName,String accountNumber,String accountHolder){}
  public record WithdrawalView(Long id,long amount,String bankName,String accountNumber,String accountHolder,String status,String note,Instant createdAt){
    static WithdrawalView of(WithdrawalRequest r){return new WithdrawalView(r.id,r.amount,r.bankName,r.accountNumber,r.accountHolder,r.status,r.note,r.createdAt);}
  }
  @GetMapping("/api/wallet") @Transactional
  public Balance balance(@AuthenticationPrincipal Jwt jwt){WalletAccount w=wallets.locked(identity.current(jwt));return new Balance(w.demoBalance,w.demoHeld,"Chỉ có số dư mô phỏng; chưa có tiền thật");}
  @GetMapping("/api/wallet/ledger") @Transactional(readOnly=true)
  public List<LedgerView> ledger(@AuthenticationPrincipal Jwt jwt){User u=identity.current(jwt);return entries.findByWallet_User_IdOrderByCreatedAtDesc(u.id).stream().map(x->new LedgerView(x.delta,x.action,x.reference,x.createdAt)).toList();}
  @PostMapping("/api/dev/wallet/topup") @Transactional
  public Balance topup(@AuthenticationPrincipal Jwt jwt,@RequestBody Amount req){
    requireDemo();User u=identity.current(jwt);RulesV2.require(req!=null&&req.amount()>0&&req.amount()<=2_000_000,"Nạp giả 1đ đến 2.000.000đ");
    WalletAccount w=wallets.locked(u);wallets.move(w,req.amount(),"DEMO_TOPUP","topup:"+UUID.randomUUID());
    return new Balance(w.demoBalance,w.demoHeld,"Số dư mô phỏng, không thể rút thật");
  }
  @PostMapping("/api/wallet/withdrawals") @Transactional
  public WithdrawalView request(@AuthenticationPrincipal Jwt jwt,@RequestBody WithdrawalInput input){
    User u=identity.current(jwt);RulesV2.require(u.isVerified(),"Chỉ tài khoản được xác thực mới có thể gửi yêu cầu rút");
    RulesV2.require(input!=null&&input.amount()>0&&input.amount()<=2_000_000,"Số tiền không hợp lệ");
    RulesV2.require(input.bankName()!=null&&input.bankName().length()>=2&&input.bankName().length()<=80,"Thiếu ngân hàng");
    RulesV2.require(input.accountNumber()!=null&&input.accountNumber().matches("[0-9]{6,30}"),"Số tài khoản ngân hàng không hợp lệ");
    RulesV2.require(input.accountHolder()!=null&&input.accountHolder().length()>=3&&input.accountHolder().length()<=120,"Thiếu tên chủ tài khoản");
    requireDemo(); // only mock withdrawals currently; no real money transfer
    WalletAccount w=wallets.locked(u);RulesV2.require(w.demoBalance>=input.amount(),"Số dư mô phỏng không đủ");
    WithdrawalRequest r=new WithdrawalRequest();r.user=u;r.amount=input.amount();r.bankName=input.bankName();r.accountHolder=input.accountHolder();r.accountNumber=input.accountNumber();
    withdrawals.save(r);wallets.move(w,-input.amount(),"WITHDRAWAL_HOLD","withdrawal:hold:"+r.id);
    w.demoHeld=Math.addExact(w.demoHeld,r.amount);
    audits.save(new Audit(u.id,"WITHDRAWAL_REQUEST","withdrawal:"+r.id));return WithdrawalView.of(r);
  }
  @GetMapping("/api/wallet/withdrawals") @Transactional(readOnly=true)
  public List<WithdrawalView> mine(@AuthenticationPrincipal Jwt jwt){return withdrawals.findByUser_IdOrderByCreatedAtDesc(identity.current(jwt).id).stream().map(WithdrawalView::of).toList();}
  @GetMapping("/api/finance/withdrawals") @Transactional(readOnly=true)
  public List<WithdrawalView> queue(@AuthenticationPrincipal Jwt jwt){identity.finance(jwt);return withdrawals.findByStatus("PENDING").stream().map(WithdrawalView::of).toList();}
  public record Decision(String action,String reason){}
  @PostMapping("/api/finance/withdrawals/{id}/decision") @Transactional
  public WithdrawalView decide(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestBody Decision d){
    User staff=identity.finance(jwt);WithdrawalRequest snapshot=withdrawals.findById(id).orElseThrow();
    WalletAccount w=wallets.locked(snapshot.user);WithdrawalRequest r=withdrawals.locked(id).orElseThrow();
    RulesV2.require(r.status.equals("PENDING"),"Yêu cầu đã xử lý");
    RulesV2.require(d!=null&&Set.of("REJECT","COMPLETE_DEMO").contains(d.action()),"Quyết định không hợp lệ");
    if(d.action().equals("REJECT")){
      RulesV2.require(d.reason()!=null&&d.reason().trim().length()>=10,"Cần lý do từ chối từ 10 ký tự");
      wallets.move(w,r.amount,"WITHDRAWAL_RELEASE","withdrawal:release:"+r.id);r.status="REJECTED";
    }else{requireDemo();r.status="COMPLETED";} // simulated completion, never transfers real money
    RulesV2.require(w.demoHeld>=r.amount,"Số dư đang giữ không khớp");w.demoHeld-=r.amount;
    r.note=d.reason();r.reviewerId=staff.id;r.decidedAt=Instant.now();
    audits.save(new Audit(staff.id,"WITHDRAWAL_"+r.status,"withdrawal:"+r.id));return WithdrawalView.of(r);
  }
  private void requireDemo(){if(!mock||env.acceptsProfiles(org.springframework.core.env.Profiles.of("prod")))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Chỉ dành cho môi trường thử nghiệm");}
}
