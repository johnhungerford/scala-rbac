import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

import scala.util.Try

object Main {

    def main( args : Array[ String ] ) : Unit = {

        val port : Int = Try( args( 0 ).toInt ).getOrElse( 9000 )

        val server = new Server ( port )
        val context = new WebAppContext()

        context.setContextPath( "/" )
        context.setResourceBase( "src/main/webapp" )
        context.setInitParameter( ScalatraListener.LifeCycleKey, "org.hungerford.scalarbac.example.ScalatraInit")
        context.addEventListener( new ScalatraListener )

        server.setHandler( context )
        server.start()
        server.join()

    }
}
