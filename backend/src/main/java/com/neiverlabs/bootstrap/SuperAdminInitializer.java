package com.neiverlabs.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Order(20)
public class SuperAdminInitializer implements ApplicationRunner {

  private final SuperAdminCreator creator;

  public SuperAdminInitializer(SuperAdminCreator creator) {
    this.creator = creator;
  }

  @Override
  public void run(ApplicationArguments args) {
    creator.createIfMissing();
  }
}
