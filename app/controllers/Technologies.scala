package controllers

import play.api.mvc._
import concurrent.ExecutionContext.Implicits.global
import models.Technology
import scala.concurrent.Future
import data._

object Technologies extends Controller with HomingPigeonPowers {

  def index = Action.async {
    TechnologyApi.all.map { response =>
        Ok(views.html.index(response))
      }
  }

  def add = Action {
    Ok(views.html.add(Technology.form))
  }

  def performAdd = Action.async { implicit request =>
    Technology.form.bindFromRequest.fold(
      errors     => Future(BadRequest(views.html.add(errors))),
      technology => TechnologyApi.insert(technology).map(flyHome)
    )
  }

  def edit(id: String) = Action.async {
    TechnologyApi.single(id) map {
      case Some(technology) => {
        val form = Technology.form.fill(technology)
        Ok(views.html.edit(id, form))
      }
      case _ => flyHome()
    }
  }

  def performEdit(id: String) = Action.async { implicit request =>
    Technology.form.bindFromRequest.fold(
      errors     => Future(BadRequest(views.html.edit(id, errors))),
      technology => TechnologyApi.update(id, technology).map(flyHome)
    )
  }

  def performDelete(id: String) = Action.async {
    TechnologyApi.delete(id).map(flyHome)
  }
}

