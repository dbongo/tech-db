package controllers

import play.api.mvc.Results

trait HomingPigeonPowers {

  val flyHome = (_:Any) => Results.Redirect(routes.Technologies.index())

}
