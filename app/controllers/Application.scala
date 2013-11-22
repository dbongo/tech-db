package controllers

import play.api.mvc._
import tasks.ES
import com.sksamuel.elastic4s.ElasticDsl._
import concurrent.ExecutionContext.Implicits.global
import models.Technology
import scala.concurrent.Future
import com.sksamuel.elastic4s.source.ObjectSource
import data.{Exporter, Importer}
import java.io.File
import com.sksamuel.elastic4s.SearchType

object Application extends Controller {

  import ES.DefaultReaders._
  import ES.Implicits._

  def doIndex() = Action.async {
    ES.client.execute(search in "technologies" size 99999)
      .map(_.resultsAs[Technology])
      .fallbackTo(Future(List.empty)).map { response =>
        Ok(views.html.index(response))
      }
  }

  def doSearch() = Action.async { request =>
    request.getQueryString("q").flatMap { case q =>
      if(q.trim.isEmpty) {
        None
      } else {
        val terms = q.split(" ").map(term("_all", _))
        val result = ES.client.execute(search in "technologies" bool must(terms:_*)).map { response =>
          val hits = response.resultsAs[Technology]
          Ok(views.html.index(hits))
        }

        Some(result)
      }

    } getOrElse {
      Future(Redirect(routes.Application.doIndex))
    }
  }

  def doTag(tag: String) = Action.async {
    ES.client.execute(search in "technologies" filter(termFilter("tags", tag)) size 99999 ).map { response =>
      val hits = response.resultsAs[Technology]
      Ok(views.html.index(hits))
    }
  }

  def viewAdd = Action {
    Ok(views.html.add(Technology.form))
  }

  def doAdd = Action.async { implicit request =>
    Technology.form.bindFromRequest.fold(
      errors => Future(BadRequest(views.html.add(errors))),
      technology => {
        val newId = ES.id
        ES.client.execute(index into "technologies/technology" id newId source ObjectSource(technology.copy(id = newId)))
          .map(_ => Redirect(routes.Application.doIndex))
      }
    )
  }

  def viewEdit(techId: String) = Action.async {
    ES.client.get(techId from "technologies/technology").map { response =>
      response.sourceAs[Technology].map { technology =>
        Ok(views.html.edit(techId, Technology.form.fill(technology)))
      } getOrElse {
        Redirect(routes.Application.doIndex)
      }
    }
  }

  def doEdit(techId: String) = Action.async { implicit request =>
    Technology.form.bindFromRequest.fold(
      errors => Future(BadRequest(views.html.edit(techId, errors))),
      technology => {
        ES.client.execute(index into "technologies/technology" id techId source ObjectSource(technology.copy(id = techId))).map { response =>
          Redirect(routes.Application.doIndex)
        }
      }
    )
  }

  def doDelete(techId: String) = Action.async {
    ES.client.delete("technologies/technology" -> techId).map { response =>
      Redirect(routes.Application.doIndex)
    }
  }

  def doStatus(status: String) = Action.async {

    ES.client.execute(search in "technologies" bool must(term("status", status))  size 99999).map { response =>
      val hits = response.resultsAs[Technology]
      Ok(views.html.index(hits))
    }
  }

  def viewView(id: String) = Action.async {

    ES.client.get(id from "technologies/technology").map { response =>

      response.sourceAs[Technology].map { technology =>
        Ok(views.html.view(technology))
      } getOrElse {
        Redirect(routes.Application.doIndex())
      }
    }
  }

  def viewImport = Action {
    Ok(views.html.importer())
  }

  def doImport = Action.async(parse.multipartFormData) { request =>
    request.body.file("file").map(_.ref.file).map { file =>
      ES.client.bulk(Importer.convert(file).map { technology =>
        index into "technologies/technology" id technology.id source ObjectSource(technology)
      }:_*).map { response =>
        Redirect(routes.Application.doIndex)
      }
    } getOrElse {
      Future(Redirect(routes.Application.doIndex))
    }
  }

  def doExport = Action.async {
    ES.client.execute(search in "technologies" size 99999).map { response =>
      val hits = response.resultsAs[Technology]
      val export = Exporter.convert(hits)
      val temp = File.createTempFile(s"export-${System.currentTimeMillis}", ".csv")
      val writer = new java.io.PrintWriter(temp)

      writer.write(export)
      writer.close

      Ok.sendFile(temp)
    }
  }
}

