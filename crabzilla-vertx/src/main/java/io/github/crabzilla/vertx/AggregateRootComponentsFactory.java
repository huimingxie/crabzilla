package io.github.crabzilla.vertx;

import io.github.crabzilla.model.*;
import io.github.crabzilla.stack.StringHelper;
import io.github.crabzilla.vertx.repositories.EntityUnitOfWorkRepository;
import io.github.crabzilla.vertx.verticles.EntityCommandHandlerVerticle;
import io.github.crabzilla.vertx.verticles.EntityCommandRestVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;
import lombok.val;
import net.jodah.expiringmap.ExpiringMap;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface AggregateRootComponentsFactory<A extends Aggregate> {

  Class<A> clazz();
  Supplier<A> supplierFn() ;
  BiFunction<DomainEvent, A, A> stateTransitionFn() ;
  Function<EntityCommand, List<String>> cmdValidatorFn() ;
  BiFunction<EntityCommand, Snapshot<A>, CommandHandlerResult> cmdHandlerFn() ;
  JDBCClient jdbcClient();
  Vertx vertx();

  // default impl

  default SnapshotPromoter<A> snapshotPromoter() {
    return new SnapshotPromoter<>(supplierFn(),
            instance -> new StateTransitionsTracker<>(instance, stateTransitionFn()));
  }

  default EntityCommandRestVerticle<A> restVerticle() {
    return new EntityCommandRestVerticle<>(clazz());
  }

  default EntityCommandHandlerVerticle<A> cmdHandlerVerticle() {

    final ExpiringMap<String, Snapshot<A>> cache = ExpiringMap.builder()
            .expiration(5, TimeUnit.MINUTES)
            .maxSize(10_000)
            .build();

    val circuitBreaker = CircuitBreaker.create(StringHelper.circuitBreakerId(clazz()), vertx(),
            new CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    );

    return new EntityCommandHandlerVerticle<>(clazz(), supplierFn().get(), cmdHandlerFn(),
            cmdValidatorFn(), snapshotPromoter(), uowRepository(), cache, circuitBreaker);
  }

  default EntityUnitOfWorkRepository uowRepository() {
    return new EntityUnitOfWorkRepository(clazz(), jdbcClient());
  }

}
