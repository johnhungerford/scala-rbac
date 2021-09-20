package org.hungerford.rbac

/**
 * A bearer of [[Role]]s.
 */
trait User {
    private val outerThis : User = this

    /**
     * Defines user permissions
     */
    val roles : Role

    /**
     * Determines whether or not a given thing (type Permissible) is permitted.
     *
     * Determined by `this.roles`
     *
     * @see [[Permission.permits(Permissible):Boolean]]
     * @param permissible Permissible: thing you want to know is permitted or not
     * @return Boolean: whether or not it is permitted
     */
    def can( permissible : Permissible ) : Boolean = roles.can( permissible )

    /**
     * Are a set of permissibles permitted by this User?
     *
     * @see [[Permission.permits(PermissibleSet):Boolean]]
     * @param permissible Permissible
     * @return Boolean
     */
    def can( permissibles: PermissibleSet ) : Boolean = permissibles match {
        case _ : AllPermissibles => permissibles.permissibles.forall {
            case Left( permissible ) => can( permissible )
            case Right( permissibleSet ) => can( permissibleSet )
        }
        case _ : AnyPermissibles => permissibles.permissibles.exists {
            case Left( permissible ) => can( permissible )
            case Right( permissibleSet ) => can( permissibleSet )
        }
    }
}
