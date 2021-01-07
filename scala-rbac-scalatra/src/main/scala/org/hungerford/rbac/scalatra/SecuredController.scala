package org.hungerford.rbac.scalatra

import org.hungerford.rbac.{Permissible, User}
import org.scalatra.ScalatraServlet

trait SecuredController extends ScalatraServlet {

    val authHeaderKey : String

    def authenticate( authHeader : String ) : User

    def authenticate : User = {
        authenticate( request.header( authHeaderKey ).getOrElse( throw new Exception() ) )
    }

    def secure[ T ]( operation : Permissible )( handler : User => T ) : T = {
        implicit val user : User = authenticate

        operation.secure( handler( user ) )
    }

    def withUser[ T ]( handler : User => T ) : T = {
        implicit val user : User = authenticate

        handler( user )
    }

    //    get( "/" )( withUser { implicit user =>
    //        CorpusAccess( Collection, AddDocument ).secure {
    //            println( "hello" )
    //        }
    //    } )

}
