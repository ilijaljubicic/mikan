package controllers

import com.google.inject.AbstractModule
import play.{Configuration, Environment}

class OnStartModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure() {
    bind(classOf[OnServerStart]).asEagerSingleton
  }

}