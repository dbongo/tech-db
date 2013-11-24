package data

import com.sksamuel.elastic4s.ElasticDsl._
import models.Technology
import scala.concurrent.Future
import com.sksamuel.elastic4s.BulkCompatibleDefinition
import concurrent.ExecutionContext.Implicits.global
import org.elasticsearch.search.SearchHit
import java.util
import data.ES.{SourceReader, SearchHitReader}
import ES.Implicits._
import org.joda.time.DateTime
import com.sksamuel.elastic4s.source.ObjectSource
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.databind.SerializationFeature
import org.elasticsearch.search.sort.SortOrder
import scala.Some

object TechnologyApi {

  private val MAX_RESULTS = 99999

  configureSerialiser

  def all = {
    executeSearch(search in "technologies")
  }

  def count = {
    executeCount(countall from "technologies" types "technology")
  }

  def single(id: String) = {
    executeGet(id from "technologies/technology")
  }

  def singleByName(name: String) = {
    executeSearchByName(search in "technologies" bool must(field("name", name)) limit 1)
  }

  def query(input: String) = {
    val terms = input.split(" ").map(term("_all", _))
    executeSearch(search in "technologies" bool must(terms:_*))
  }

  def forTag(tag: String) = {
    executeSearch(search in "technologies" filter termFilter("tags", tag))
  }

  def forStatus(status: String) = {
    executeSearch(search in "technologies" bool must(term("status", status)))
  }

  def insert(technology: Technology) = {
    val newId = ES.id
    executeIndex(index into "technologies/technology" id newId source ObjectSource(technology.withId(newId)))
  }

  def update(originalId: String, technology: Technology) = {
    single(originalId).map(_.map(_.added)).map {
      case Some(added) => Right(executeIndex(
        index into "technologies/technology" id originalId source ObjectSource(
          technology.copy(id = originalId, added = added)
        )
      ))
      case _ => Left()
    }
  }

  def insertMany(technologies: Seq[Technology]) = {
    val insertions = technologies.map { technology =>
      index into "technologies/technology" id technology.id source ObjectSource(technology)
    }

    executeBulk(insertions:_*)
  }

  def delete(id: String) = {
    executeDelete("technologies/technology" -> id)
  }

  def deleteAll = {
    executeDelete("technologies")
  }

  def setPriority(originalId: String, priority: Boolean) = {
    import com.sksamuel.elastic4s.ElasticDsl.{update => doUpdate} // we want to override the update DSL
    executeUpdate(doUpdate id originalId in "technologies/technology" script s"ctx._source.priority = $priority")
  }

  def priorityOnly = {
    executeSearch(search in "technologies" bool must(field("priority", true)))
  }

  implicit val technologySourceReader = new SourceReader[Technology]{
    def read(source: util.Map[String, Object]): Technology = {
      Technology(
        source.getAs[String]("id").get,
        source.getAs[String]("name").get,
        source.getAs[String]("description").get,
        source.getAs[Seq[String]]("tags").get,
        source.getAs[String]("homePage").flatMap(Option(_)),
        source.getAs[String]("status").get,
        source.getAs[Boolean]("priority").get,
        source.getAs[DateTime]("added").get,
        source.getAs[DateTime]("lastModified").get
      )
    }
  }

  implicit val technologyHitReader = new SearchHitReader[Technology]{
    def read(searchHit: SearchHit) : Technology = {
      technologySourceReader.read(searchHit.getSource)
    }
  }

  private def executeSearch(definition: SearchDefinition) = {
    // TODO Sort kinda works but not completely, investigate
    ES.client.execute(definition size MAX_RESULTS sort(by field "name" order SortOrder.ASC))
      .map(_.resultsAs[Technology])
      .fallbackTo(Future(List.empty))
  }

  private def executeSearchByName(definition: SearchDefinition) = {
    executeSearch(definition).map(_.headOption)
  }
  
  private def executeIndex(definition: IndexDefinition) = {
    ES.client.execute(definition)
  }

  private def executeUpdate(definition: UpdateDefinition) = {
    ES.client.execute(definition)
  }

  private def executeGet(definition: GetDefinition) = {
    ES.client.get(definition).map(_.sourceAs[Technology]).fallbackTo(Future(None))
  }

  private def executeBulk(definitions: BulkCompatibleDefinition*) = {
    ES.client.bulk(definitions:_*)
  }

  private def executeDelete(definition: DeleteByIdDefinition) = {
    ES.client.delete(definition)
  }

  private def executeDelete(definition: DeleteIndexDefinition) = {
    ES.client.deleteIndex(definition)
  }

  private def executeCount(definition: CountDefinition) = {
    ES.client.execute(definition).map(_.getCount)
  }

  private def configureSerialiser {
    ObjectSource.mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    ObjectSource.mapper.registerModule(new JodaModule)
  }

}
