/*
 * This file is part of the \BlueLaTeX project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bluelatex.persistence

import akka.actor.{
  Actor,
  ActorRef,
  ActorSystem,
  Props
}

import akka.pattern.ask

import scala.concurrent.duration._
import scala.concurrent.Await

import akka.testkit.{
  TestActors,
  TestKit,
  ImplicitSender
}
import org.scalatest.FlatSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

import scala.collection.{ mutable => mu }

import better.files._

class FsStoreTest(_system: ActorSystem)
    extends TestKit(_system)
    with ImplicitSender
    with FlatSpecLike
    with Matchers
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("fsstore-test-system"))

  val data = Container("toto")(
    mu.Set(
      Container("tata")(
        mu.Set(
          Leaf("titi")("piouc"),
          Leaf("tutu")("plop"))),
      Leaf("tete")("gloups")))

  val base = File.newTempDir()

  val toto = base / "toto"
  val tata = toto / "tata"
  val titi = tata / "titi"
  val tutu = tata / "tutu"
  val tete = toto / "tete"

  val actor = system.actorOf(Props(classOf[FsStore], base), base.name)

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "a directory structure" should "be saved to FS when Save message is sent" in {
    actor ! Save(data)
    expectMsg(Unit)

    titi.contentAsString should be("piouc")
    tutu.contentAsString should be("plop")
    tete.contentAsString should be("gloups")

  }

  it should "be loaded from FS when Load message is sent" in {
    actor ! Load(List("toto"))

    val cont = expectMsgClass(classOf[Container])

    cont.name should be("toto")
    cont.elements.foreach {
      case c @ Container("tata") =>
        c.elements should be(mu.Set(Leaf("titi")("piouc"), Leaf("tutu")("plop")))
      case l @ Leaf("tete") =>
        l.content should be("gloups")
      case _ =>
        fail("Unexpected data")
    }
  }

  it should "be deleted from FS when Delete message is sent" in {

    actor ! Delete(List("toto", "tata"))

    expectMsg(Unit)

    toto.isDirectory should be(true)
    tata.exists should be(false)
    titi.exists should be(false)
    tutu.exists should be(false)
    tete.contentAsString should be("gloups")

  }

}
