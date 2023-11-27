package com.rockthejvm.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import pureconfig.error.CannotConvert
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port

// Implement given configReader: ConfigReader[EmberConfig]
final case class EmberConfig(host: Host, port: Port) derives ConfigReader

object EmberConfig {
  // need given ConfigReader[Host] + given ConfigRead[Port]
  // so that compiler can successfully generates ConfigReader[EmberConfig] (because host is Host type and port is Port type now)
  given hostReader: ConfigReader[Host] = /* From String to Host */ ConfigReader[String].emap { hostString =>
    // 注意Option的toRight方法比pattern matching要简洁得多！！
    Host.fromString(hostString) match {
      case None       => Left(CannotConvert(hostString, Host.getClass.toString, s"Invalid host string: $hostString")) // error, return a Left
      case Some(host) => Right(host)
    }
  }

  given portReader: ConfigReader[Port] = /* From Int to Port */ ConfigReader[Int].emap { portInt =>
    // 注意Option的toRight方法比pattern matching要简洁得多！！
    Port.fromInt(portInt).toRight(CannotConvert(portInt.toString, Port.getClass.toString, s"Invalid port number: $portInt"))
  }
}
