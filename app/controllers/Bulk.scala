package controllers

import play.api.mvc.{Action, Controller}
import data.{TechnologyApi, CSV}
import java.io.File
import concurrent.ExecutionContext.Implicits.global

object Bulk extends Controller with HomingPigeonPowers {

  def in = Action {
    Ok(views.html.importer())
  }

  def performIn = Action(parse.multipartFormData) { request =>
    request.body.file("file").map(_.ref.file).map { file =>
      val technologies = CSV.from(file)
      TechnologyApi.insertMany(technologies)
    }

    flyHome()
  }

  def out = Action.async {
    TechnologyApi.all.map { technologies =>

      val export = CSV.to(technologies)
      val temp = File.createTempFile(s"export-${System.currentTimeMillis}", ".csv")

      val writer = new java.io.PrintWriter(temp)

      writer.write(export)
      writer.close

      Ok.sendFile(temp, onClose = () => temp.delete())
    }
  }

}
