package com.easyhome.common;

import java.time.Instant;

public final class AccountLockedException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final String reason;
  private final Instant until;

  public AccountLockedException(String reason, Instant until) {
    super("Tài khoản đang bị khóa");
    this.reason = reason;
    this.until = until;
  }

  public String reason() { return reason; }
  public Instant until() { return until; }
}
