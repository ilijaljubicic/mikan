package controllers

import com.google.inject.AbstractModule
import play.{Configuration, Environment}

class OnStartModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure() {
    //  val helloConfiguration: Configuration =configuration.getConfig("hello").getOrElse(Configuration.empty)

    //    bind(Service.class).to(ServiceImpl.class).in(Singleton.class)
    //    bind(CreditCardPaymentService.class)
    //    bind(PaymentService.class).to(CreditCardPaymentService.class)
    //    bindConstant().annotatedWith(Names.named("port")).to(8080)

    bind(classOf[OnServerStart]).asEagerSingleton
  }


}