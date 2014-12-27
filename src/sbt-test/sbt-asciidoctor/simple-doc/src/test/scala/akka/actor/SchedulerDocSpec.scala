/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.actor

import language.postfixOps

//#imports1
import scala.concurrent.duration._

//#imports1

class SchedulerDocSpec extends AkkaSpec(Map("akka.loglevel" -> "INFO")) {
  "schedule a one-off task" in {
    //#schedule-one-off-message
    //Use the system's dispatcher as ExecutionContext

    //Schedules to send the "foo"-message to the testActor after 50ms
    system.scheduler.scheduleOnce(50 milliseconds, testActor, "foo")
    //#schedule-one-off-message

    expectMsg(1 second, "foo")

    //#schedule-one-off-thunk
    //Schedules a function to be executed (send a message to the testActor) after 50ms
    system.scheduler.scheduleOnce(50 milliseconds) {
      testActor ! System.currentTimeMillis
    }
    //#schedule-one-off-thunk

  }

  "schedule a recurring task" in {
    new AnyRef {
      //#schedule-recurring
      val Tick = "tick"
      class TickActor extends Actor {
        def receive = {
          case Tick => //Do something
        }
      }
      val tickActor = system.actorOf(Props(classOf[TickActor], this))
      //Use system's dispatcher as ExecutionContext

      //This will schedule to send the Tick-message
      //to the tickActor after 0ms repeating every 50ms
      val cancellable =
        system.scheduler.schedule(0 milliseconds,
          50 milliseconds,
          tickActor,
          Tick)

      //This cancels further Ticks to be sent
      cancellable.cancel()
      //#schedule-recurring
      system.stop(tickActor)
    }
  }
}
