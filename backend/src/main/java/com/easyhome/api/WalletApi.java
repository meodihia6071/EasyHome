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
  private final Withdrawals withdrawals;private final Audits audits;private final Environment env;private final Payments payments;
  private final boolean mock;
  WalletApi(WalletService w,WalletEntries e,Identity i,Withdrawals req,Audits a,Environment env,Payments payments,
      @Value("${app.mock-payments:false}") boolean mock){wallets=w;entries=e;identity=i;withdrawals=req;audits=a;this.env=env;this.payments=payments;this.mock=mock;}
  public record Balance(long available,long held){}
  public record LedgerView(long delta,String action,String reference,Instant at){}
  public record Amount(long amount){}
  public record DepositView(Long id,long amount,String reference,String status,Instant createdAt){}
  public record WithdrawalInput(long amount,String bankName,String accountNumber,String accountHolder){}
  public record WithdrawalView(Long id,long amount,String bankName,String accountNumber,String accountHolder,String status,String note,Instant createdAt){
    static WithdrawalView of(WithdrawalRequest r){return new WithdrawalView(r.id,r.amount,r.bankName,r.accountNumber,r.accountHolder,r.status,r.note,r.createdAt);}
  }
  @GetMapping("/api/wallet") @Transactional
  public Balance balance(@AuthenticationPrincipal Jwt jwt){WalletAccount w=wallets.locked(identity.current(jwt));return new Balance(w.availableBalance,w.heldBalance);}
  @GetMapping("/api/wallet/ledger") @Transactional(readOnly=true)
  public List<LedgerView> ledger(@AuthenticationPrincipal Jwt jwt){User u=identity.current(jwt);return entries.findByWallet_User_IdOrderByCreatedAtDesc(u.id).stream().filter(x->!x.action.startsWith("DEMO_")).map(x->new LedgerView(x.delta,x.action,x.reference,x.createdAt)).toList();}
  @PostMapping("/api/dev/wallet/topup") @Transactional
  public Balance topup(@AuthenticationPrincipal Jwt jwt,@RequestBody Amount req){
    requireDemo();User u=identity.current(jwt);RulesV2.require(req!=null&&req.amount()>0&&req.amount()<=2_000_000,"Nạp giả 1đ đến 2.000.000đ");
    WalletAccount w=wallets.locked(u);wallets.move(w,req.amount(),"DEMO_TOPUP","topup:"+UUID.randomUUID());
    return new Balance(w.availableBalance,w.heldBalance);
  }
  @GetMapping("/api/wallet/deposits") @Transactional(readOnly=true)
  public List<DepositView> deposits(@AuthenticationPrincipal Jwt jwt){return payments.findByPayer_IdAndMethodOrderByCreatedAtDesc(identity.current(jwt).id,"VIETQR_TOPUP").stream().map(p->new DepositView(p.id,p.amount,p.reference,p.status,p.createdAt)).toList();}
  @PostMapping("/api/wallet/deposits") @Transactional
  public DepositView deposit(@AuthenticationPrincipal Jwt jwt,@RequestBody Amount input){
    User user=identity.current(jwt);RulesV2.require(input!=null&&input.amount()>=10000&&input.amount()<=100000000,"Số tiền nạp phải từ 10.000 đến 100.000.000 đồng");
    PaymentTransaction p=new PaymentTransaction();p.payer=user;p.amount=input.amount();p.method="VIETQR_TOPUP";p.status="PENDING";p.reference="EHN "+UUID.randomUUID().toString().replace("-","").substring(0,18).toUpperCase(Locale.ROOT);
    payments.saveAndFlush(p);return new DepositView(p.id,p.amount,p.reference,p.status,p.createdAt);
  }
  @PostMapping("/api/wallet/deposits/{id}/submitted") @Transactional
  public DepositView submittedDeposit(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    User user=identity.current(jwt);PaymentTransaction p=payments.locked(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    if(!p.payer.getId().equals(user.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    RulesV2.require("VIETQR_TOPUP".equals(p.method),"Yêu cầu nạp không hợp lệ");
    if("PENDING".equals(p.status))p.status="SUBMITTED";
    else RulesV2.require("SUBMITTED".equals(p.status),"Giao dịch đã được xử lý");
    return new DepositView(p.id,p.amount,p.reference,p.status,p.createdAt);
  }
  @GetMapping("/api/admin/wallet/deposits") @Transactional(readOnly=true)
  public List<DepositView> pendingDeposits(@AuthenticationPrincipal Jwt jwt){identity.admin(jwt);return payments.findByMethodAndStatusOrderByCreatedAtAsc("VIETQR_TOPUP","SUBMITTED").stream().map(p->new DepositView(p.id,p.amount,p.reference,p.status,p.createdAt)).toList();}
  @PostMapping("/api/admin/wallet/deposits/{id}/approve") @Transactional
  public DepositView approveDeposit(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    User staff=identity.admin(jwt);PaymentTransaction p=payments.locked(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    RulesV2.require("VIETQR_TOPUP".equals(p.method)&&"SUBMITTED".equals(p.status),"Người dùng chưa xác nhận đã chuyển khoản hoặc giao dịch đã được xử lý");
    WalletAccount w=wallets.locked(p.payer);wallets.move(w,p.amount,"BANK_TOPUP", "vietqr:deposit:"+p.id);p.status="PAID";p.confirmedAt=Instant.now();
    audits.save(new Audit(staff.id,"APPROVE_BANK_TOPUP","payment:"+p.id));return new DepositView(p.id,p.amount,p.reference,p.status,p.createdAt);
  }
  @PostMapping("/api/wallet/withdrawals") @Transactional
  public WithdrawalView request(@AuthenticationPrincipal Jwt jwt,@RequestBody WithdrawalInput input){
    User u=identity.current(jwt);RulesV2.require(u.isVerified(),"Chỉ tài khoản được xác thực mới có thể gửi yêu cầu rút");
    RulesV2.require(input!=null&&input.amount()>0&&input.amount()<=2_000_000,"Số tiền không hợp lệ");
    RulesV2.require(input.bankName()!=null&&input.bankName().length()>=2&&input.bankName().length()<=80,"Thiếu ngân hàng");
    RulesV2.require(input.accountNumber()!=null&&input.accountNumber().matches("[0-9]{6,30}"),"Số tài khoản ngân hàng không hợp lệ");
    RulesV2.require(input.accountHolder()!=null&&input.accountHolder().length()>=3&&input.accountHolder().length()<=120,"Thiếu tên chủ tài khoản");
    WalletAccount w=wallets.locked(u);RulesV2.require(w.availableBalance>=input.amount(),"Số dư khả dụng không đủ");
    WithdrawalRequest r=new WithdrawalRequest();r.user=u;r.amount=input.amount();r.bankName=input.bankName();r.accountHolder=input.accountHolder();r.accountNumber=input.accountNumber();
    withdrawals.save(r);
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
    RulesV2.require(d!=null&&Set.of("REJECT","APPROVE").contains(d.action()),"Quyết định không hợp lệ");
    if(d.action().equals("REJECT")){
      RulesV2.require(d.reason()!=null&&d.reason().trim().length()>=10,"Cần lý do từ chối từ 10 ký tự");
      r.status="REJECTED";
    }else{
      RulesV2.require(w.availableBalance>=r.amount,"Số dư không đủ để duyệt yêu cầu rút");
      wallets.move(w,-r.amount,"WITHDRAWAL_APPROVED","withdrawal:approved:"+r.id);r.status="COMPLETED";
    }
    r.note=d.reason();r.reviewerId=staff.id;r.decidedAt=Instant.now();
    audits.save(new Audit(staff.id,"WITHDRAWAL_"+r.status,"withdrawal:"+r.id));return WithdrawalView.of(r);
  }
  private void requireDemo(){if(!mock||env.acceptsProfiles(org.springframework.core.env.Profiles.of("prod")))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Chỉ dành cho môi trường thử nghiệm");}
}
