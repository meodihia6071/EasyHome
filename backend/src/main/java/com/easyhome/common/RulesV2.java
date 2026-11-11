package com.easyhome.common;
import com.easyhome.common.Rules;
import java.util.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

public class RulesV2 {
  public static void require(boolean condition,String message){if(!condition)throw new Rules.BusinessException(message);}
}
