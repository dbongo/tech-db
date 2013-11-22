package data

import com.sksamuel.elastic4s.ElasticDsl._
import models.Technology
import scala.concurrent.Future
import com.sksamuel.elastic4s.source.ObjectSource
import com.sksamuel.elastic4s.BulkCompatibleDefinition
import concurrent.ExecutionContext.Implicits.global


object TechnologyApi {

  import ES.Implicits._
  import TechnologyReaders._

  private val MAX_RESULTS = 99999

  def all = {
    executeSearch(search in "technologies" size MAX_RESULTS)
  }

  def single(id: String) = {
    executeGet(id from "technologies/technology")
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
    executeIndex(index into "technologies/technology" id originalId source ObjectSource(technology.withId(originalId)))
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
    executeDelete("technologies/technology")
  }

  private def executeSearch(definition: SearchDefinition) = {
    ES.client.execute(definition size MAX_RESULTS)
      .map(_.resultsAs[Technology])
      .fallbackTo(Future(List.empty))
  }
  
  private def executeIndex(definition: IndexDefinition) = {
    ES.client.execute(definition)
  }

  private def executeGet(definition: GetDefinition) = {
    ES.client.get(definition)
      .map(_.sourceAs[Technology])
      .fallbackTo(Future(None))
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
}
