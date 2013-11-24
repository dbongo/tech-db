package controllers

import play.api.mvc.{Action, Controller}
import data.TechnologyApi
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global

object Query extends Controller with HomingPigeonPowers {

  def query = Action.async { request =>
    request.getQueryString("q")
      .collect({ case q if !q.trim.isEmpty => q })
      .map { q =>
        for(
          count   <- TechnologyApi.count;
          results <- TechnologyApi.query(q)
        ) yield Ok(views.html.index(results, count))
      } getOrElse {
        Future(flyHome())
      }
  }

  def forTag(tag: String) = Action.async {
    for(
      count   <- TechnologyApi.count;
      results <- TechnologyApi.forTag(tag)
    ) yield Ok(views.html.index(results, count))
  }

  def forStatus(status: String) = Action.async {
    for(
      count   <- TechnologyApi.count;
      results <- TechnologyApi.forStatus(status)
    ) yield Ok(views.html.index(results, count))
  }

  def priorityOnly = Action.async {
    for(
      count   <- TechnologyApi.count;
      results <- TechnologyApi.priorityOnly
    ) yield Ok(views.html.index(results, count))
  }
}
