package org.hungerford.rbac.scalatra

import org.hungerford.rbac.exceptions.RbacException

package object exceptions {

    class AuthenticationException( msg : String ) extends RbacException( msg : String )

    class MissingAuthenticationHeaderException( authHeader : String )
      extends AuthenticationException( s"Missing authentication header: $authHeader" )

    class FailedAuthenticationException( reason : String )
      extends AuthenticationException( s"Unable to authenticate: $reason" )

}
