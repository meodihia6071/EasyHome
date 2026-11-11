package com.easyhome.service;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WalletService {
  private final Users users;private final Wallets wallets;private final WalletEntries ledger;
  WalletService(Users u,Wallets w,WalletEntries l){users=u;wallets=w;ledger=l;}
  @Transactional
  public WalletAccount locked(User u){
    User managed=users.locked(u.getId()).orElseThrow();
    Optional<WalletAccount> old=wallets.lockedByUser(u.getId());
    if(old.isPresent())return old.get();
    WalletAccount w=new WalletAccount();w.user=managed;return wallets.saveAndFlush(w);
  }
  public void move(WalletAccount w,long delta,String action,String ref){
    if(ledger.existsByReference(ref))return; // same operation must not mutate twice
    RulesV2.require(delta<=0 || Long.MAX_VALUE-w.demoBalance>=delta,"Số dư vượt giới hạn");
    RulesV2.require(delta>=0 || w.demoBalance>=-delta,"Số dư mô phỏng không đủ");
    w.demoBalance=Math.addExact(w.demoBalance,delta);
    WalletLedger e=new WalletLedger();e.wallet=w;e.delta=delta;e.action=action;e.reference=ref;ledger.save(e);
    wallets.saveAndFlush(w);
  }
  public void refund(User u,long amount,long orderId){
    WalletAccount w=locked(u);move(w,amount,"REFUND_LISTING","refund:order:"+orderId);
  }
}
