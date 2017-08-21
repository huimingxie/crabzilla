package io.github.crabzilla.example1.customer;


import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;
import io.github.crabzilla.example1.services.SampleInternalService;
import io.github.crabzilla.model.*;
import io.github.crabzilla.vertx.AggregateRootComponentsFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class CustomerFactory implements AggregateRootComponentsFactory<Customer> {

  private final Vertx vertx;
  private final JDBCClient jdbcClient;
  private final Customer seedValue;

  @Inject
  public CustomerFactory(SampleInternalService service, Vertx vertx, JDBCClient jdbcClient) {
    this.seedValue = new Customer(service, null, null, false, null);
    this.vertx = vertx;
    this.jdbcClient = jdbcClient;
  }


  @Override
  public Class<Customer> clazz() {
    return Customer.class;
  }

  @Override
  public Supplier<Customer> supplierFn() {
    return () -> seedValue;
  }

  @Override
  public BiFunction<DomainEvent, Customer, Customer> stateTransitionFn() {
    return new CustomerFunctions.StateTransitionFn();
  }

  @Override
  public Function<EntityCommand, List<String>> cmdValidatorFn() {
    return new CustomerFunctions.CommandValidatorFn();
  }

  @Override
  public BiFunction<EntityCommand, Snapshot<Customer>, CommandHandlerResult> cmdHandlerFn() {
    return new CustomerFunctions.CommandHandlerFn(instance ->
            new StateTransitionsTracker<>(instance, stateTransitionFn()));
  }

  @Override
  public JDBCClient jdbcClient() {
    return jdbcClient;
  }

  @Override
  public Vertx vertx() {
    return vertx;
  }

}