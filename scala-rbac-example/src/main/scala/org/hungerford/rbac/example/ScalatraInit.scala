package org.hungerford.rbac.example

import org.scalatra.LifeCycle

import javax.servlet.ServletContext

class ScalatraInit extends LifeCycle {
    override def init( context : ServletContext ) : Unit = {
        context.mount( DocumentResourceController, "/*" )
    }

    // Scalatra callback to close out resources
    override def destroy( context : ServletContext ) : Unit = {
        super.destroy( context )
    }
}
