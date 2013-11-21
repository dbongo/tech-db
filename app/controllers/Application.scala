package controllers

import play.api.mvc._
import tasks.ES
import com.sksamuel.elastic4s.ElasticDsl._
import concurrent.ExecutionContext.Implicits.global
import models.Technology
import scala.concurrent.Future
import com.sksamuel.elastic4s.source.ObjectSource

object Application extends Controller {

  import ES.DefaultReaders._
  import ES.Implicits._

  def doIndex() = Action.async {
    ES.client.execute(search in "technologies").map { response =>
      val hits = response.resultsAs[Technology]
      Ok(views.html.index(hits))
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
    ES.client.execute(search in "technologies" filter(termFilter("tags", tag)) ).map { response =>
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
      technology =>
        ES.client.execute(index into "technologies/technology" id technology.id source ObjectSource(technology))
          .map(_ => Redirect(routes.Application.doIndex))
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

    ES.client.execute(search in "technologies" bool(must(term("status", status)))).map { response =>
      val hits = response.resultsAs[Technology]
      Ok(views.html.index(hits))
    }
  }

  def viewView(id: String) = Action.async {

    println(">>>> IN VIEWVIEW")

    ES.client.get(id from "technologies/technology").map { response =>

      response.sourceAs[Technology].map { technology =>
        Ok(views.html.view(technology))
      } getOrElse {
        Redirect(routes.Application.doIndex)
      }
    }
  }
}

