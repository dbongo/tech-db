
import data.CSV
import models.Technology
import org.joda.time.DateTime
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

@RunWith(classOf[JUnitRunner])
class CSVSpec extends Specification {

  val reference = Technology("1","Test","This is a test", Seq("test1", "test2", "test3"), Some("http://www.google.com"), "New", DateTime.parse("2013-11-22T15:51:23.066Z"))

  "Importer" should {
    "convert a csv'd list of strings into technologies" in {
      val conversion = CSV.from(
        """1,Test,"This is a test",test1 test2 test3,"http://www.google.com",New,2013-11-22T15:51:23.066Z
          |2,Test,"This is a test",test1 test2 test3,"http://www.google.com",New,2013-11-22T15:51:23.066Z
          |3,Test,"This is a test",test1 test2 test3,"http://www.google.com",New,2013-11-22T15:51:23.066Z
        """.stripMargin)
      conversion.size must_== 3
      conversion(0) must_== reference.copy(id = "1")
      conversion(1) must_== reference.copy(id = "2")
      conversion(2) must_== reference.copy(id = "3")
    }

    "set none when no home page is specified" in {
      val conversion = CSV.from("1,Test,This is a test,test1 test2 test3,,New,2013-11-22T15:51:23.066Z")
      conversion.head.homePage must_== None
    }
  }

  "Exporter" should {
    "convert a list of technologies to a csv list" in {
      CSV.to(List(reference)) must_== """1,Test,"This is a test",test1 test2 test3,"http://www.google.com",New,2013-11-22T15:51:23.066Z"""
      CSV.to(List(
        reference,
        reference.copy(id = "2", homePage = None),
        reference.copy(id = "3", description = "This, is a , test")
      )) must_==
        """1,Test,"This is a test",test1 test2 test3,"http://www.google.com",New,2013-11-22T15:51:23.066Z
          |2,Test,"This is a test",test1 test2 test3,"",New,2013-11-22T15:51:23.066Z
          |3,Test,"This, is a , test",test1 test2 test3,"http://www.google.com",New,2013-11-22T15:51:23.066Z""".stripMargin
    }
  }

  "Back and forth translation" should {
    "be idempotent" in {
      val original = List(
        reference,
        reference.copy(id = "2", homePage = None),
        reference.copy(id = "3", description = "This, is a , test")
      )
      CSV.from(CSV.to(original)) must_== original
      CSV.from(CSV.to(CSV.from(CSV.to(original)))) must_== original
    }
  }
}
