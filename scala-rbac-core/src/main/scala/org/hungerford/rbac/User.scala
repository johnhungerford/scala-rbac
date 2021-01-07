package org.hungerford.rbac

trait User {
    private val outerThis : User = this

    val name : String
    val roles : Role

    def can( permissible : Permissible ) : Boolean = roles.can( permissible )

    /**
     * Copy a user and add new role. Probably should be extended
     *
     * @param newRoles
     * @return
     */
    def grant( newRoles : Role* ) : User = new User {
        val name : String = outerThis.name
        override val roles : Role = outerThis.roles + Roles( newRoles )
    }

    override def toString : String = s"User(name: $name, roles: $roles)"
}
