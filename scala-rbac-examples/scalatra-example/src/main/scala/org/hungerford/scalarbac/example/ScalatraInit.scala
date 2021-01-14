package org.hungerford.scalarbac.example

import org.hungerford.scalarbac.example.controllers.{DocumentResourceController, DocumentUserController}
import org.scalatra.LifeCycle

import javax.servlet.ServletContext

class ScalatraInit extends LifeCycle {
    override def init( context : ServletContext ) : Unit = {
        context.mount( DocumentResourceController, "/resources/*" )
        context.mount( DocumentUserController, "/users/*" )
    }

    // Scalatra callback to close out resources
    override def destroy( context : ServletContext ) : Unit = {
        super.destroy( context )
    }
}
