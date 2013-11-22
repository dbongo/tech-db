package controllers

import play.api.mvc._
import concurrent.ExecutionContext.Implicits.global
import models.Technology
import scala.concurrent.Future
import data._
import java.io.File

object Application extends Controller {


  def doIndex = Action.async {
    TechnologyApi.all.map { response =>
        Ok(views.html.index(response))
      }
  }

  def doSearch = Action.async { request =>
    request.getQueryString("q").flatMap { case q if !q.trim.isEmpty =>
      Some {
        TechnologyApi.query(q).map { results =>
          Ok(views.html.index(results))
        }
      }
    } getOrElse {
      Future(returnToHome())
    }
  }

  def doTag(tag: String) = Action.async {
    TechnologyApi.forTag(tag).map { results =>
      Ok(views.html.index(results))
    }
  }

  def viewAdd = Action {
    Ok(views.html.add(Technology.form))
  }

  def doAdd = Action.async { implicit request =>
    Technology.form.bindFromRequest.fold(
      errors => Future(BadRequest(views.html.add(errors))),
      technology => TechnologyApi.insert(technology)
        .map(returnToHome))
  }

  def viewEdit(id: String) = Action.async {
    TechnologyApi.single(id) map {
      case Some(technology) => {
        val form = Technology.form.fill(technology)
        Ok(views.html.edit(id, form))
      }
      case _ => returnToHome()
    }
  }

  def doEdit(id: String) = Action.async { implicit request =>

    Technology.form.bindFromRequest.fold(
      errors => Future(BadRequest(views.html.edit(id, errors))),
      technology => TechnologyApi.update(id, technology)
        .map(returnToHome)
    )
  }

  def doDelete(id: String) = Action.async {
    TechnologyApi.delete(id)
      .map(returnToHome)
  }

  def doStatus(status: String) = Action.async {
    TechnologyApi.forStatus(status).map { results =>
      Ok(views.html.index(results))
    }
  }

  def viewImport = Action {
    Ok(views.html.importer())
  }

  def doImport = Action(parse.multipartFormData) { request =>
    request.body.file("file").map(_.ref.file).map { file =>
      val technologies = CSV.from(file)
      TechnologyApi.insertMany(technologies)
    }

    returnToHome()
  }

  def doExport = Action.async {
    TechnologyApi.all.map { technologies =>

      val export = CSV.to(technologies)
      val temp = File.createTempFile(s"export-${System.currentTimeMillis}", ".csv")

      val writer = new java.io.PrintWriter(temp)

      writer.write(export)
      writer.close

      Ok.sendFile(temp, onClose = () => temp.delete())
    }
  }

  val returnToHome = (_:Any) => Redirect(routes.Application.doIndex())
}

