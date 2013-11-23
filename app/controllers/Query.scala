package controllers

import play.api.mvc.{Action, Controller}
import data.TechnologyApi
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global

object Query extends Controller with HomingPigeonPowers {

  def query = Action.async { request =>
    request.getQueryString("q").flatMap { case q if !q.trim.isEmpty =>
      Some {
        TechnologyApi.query(q).map { results =>
          Ok(views.html.index(results))
        }
      }
    } getOrElse {
      Future(flyHome())
    }
  }

  def forTag(tag: String) = Action.async {
    TechnologyApi.forTag(tag).map { results =>
      Ok(views.html.index(results))
    }
  }

  def forStatus(status: String) = Action.async {
    TechnologyApi.forStatus(status).map { results =>
      Ok(views.html.index(results))
    }
  }
}
