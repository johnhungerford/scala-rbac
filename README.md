# Scala RBAC

A role-based access control library for Scala

## Importing this Library

This project is not yet published in any public artifact repositories, but it can
be included easily in an sbt project by adding the following to `build.sbt`:

```scala
dependsOn( ProjectRef( uri( "https://github.com/johnhungerford/scala-rbac.git" ), "moduleName" ) )
```

where `moduleName` is the name of the module you want to import as defined in this
project's `build.sbt` file. The current library modules are:

* `rbacCore`: the base permissions library, including `Permission`, `Permissible`,
`Role`, `Resource`, and `User`

* `rbacHttp`: provides `SecureController` trait for integrating scala-rbac with REST controllers.
  
* `rbacScalatra`: provides `SecureScalatraServlet` trait for Scalatra projects

* `rbacPlay`: provides `SecureAbstractController` class for Play projects.
  
*NB: for multi-project builds, call `dependsOn` from the sub-project you need this
library in:* 
```scala
val subProject1 = project in ...

subProject1.dependsOn( ProjectRef( ... ) )
```

## Basic Usage

### Permissions and Operations

The fundamental components of `scala-rbac` are `Permission` and `Permissible`.
`Permissible` defines something that may or may not be permitted. For
the most part, a `Permissible` can be thought about as an operation, and
for this reason, the trait `Operation` which is a simple subtype of
`Permissible` can be used in its place. `Permission` grants access any
number of `Permissibles`.

The simplest way to create a `Permission` that permits an `Operation` is
as follows:

```scala
case object DoAThing extends Operation

val perm : Permission = Permission.to(DoAThing)
```

You can then validate operations by checking them with the `.permits`
method:

```scala
case object DoSomethingElse extends Operation

perm.permits(DoAThing) // true
perm.permits(DoSomethingElse) // false
```

Alternatively, you can define your own Permission class by extending
the `SimplePermission` class and overriding the `.permits` method:

```scala
object MyPerm extends SimplePermission {
    override def permits( permissible : Permissible ) : Boolean = {
        permissible match {
            case DoAthing => true
            case _ => false
        }
    }
}

MyPerm.permits(DoAThing) // true
MyPerm.permits(DoSomethingElse) // false
```

### Securing Methods

The most useful way to employ these classes is the `Permissible.secure`
method, which is used as so:

```scala
def hello(name : String = "World")( implicit ps : PermissionSource ) : Unit = DoAThing.secure {
    println( s"Hello $name!" )
}
```

The `hello` method can only be used in the context of an implicit
instance of one of the three `scala-rbac` types that provides permissions:
`Permission`, `Role`, or `User`. If that permission source does not grant
permission to `DoAThing`, `hello` will throw an `UnpermittedOperationException`:

```scala
implicit var perm = MyPerm

// outputs: Hello Erik!
hello("Erik")

// NoPermissions is a built-in Permission object that
// permits nothing
perm = NoPermissions 

// Throws UnpermittedOperationException: 
hello("David")

// Permissions can also be passed to a secured method explicitly.
// The line below uses AllPermissions, another built-in Permission
// that permits everything. So despite the inadequate implicit permission,
// it will output "Hello Larry!"
hello("Larry")( AllPermissions )

```

You can secure against multiple permissibles by composing them with `&`
or `|`:

```scala
def hello(name: String = "World")(implicit ps : PermissionSource) : Unit = {
    (DoAThing | DoSomethingSomethingElse).secure {
        println(s"Hello $name")
    }
}

implicit var perm = Permission.to(DoAThing)

// Works
hello("Matthew")

perm = Permission.to(DoSomethingElse)

// Also works
hello("Mark")
```

Either permission will permit `DoAThing | DoSomethingElse`, but
`DoAthing & DoSomethingElse` will only be permitted by a permission
that allows *both* operations:

```scala
def strictHello(name: String = "World")(implicit ps: PermissionSource): Unit = {
    (DoAThing & DoSomethingSomethingElse).secure {
        println(s"Hello $name")
    }
}

val perm1 = Permission.to(DoAThing)
val perm2 = Permission.to(DoSomethingElse)

implicit var implPerm = perm1

// Fails
strictHello("Matthew")

implPerm = perm2

// Also fails
strictHello("Mark")

implPerm = perm1 | perm2

// Works!
strictHello("Luke")
```

Note that we can combine permissions using the `|` operator as well, which
generates a new permission permitting whatever each of the two permissions allowed.

## Securing a REST operation

`scala-rbac-core` also includes `Role`, which can be composed from permissions,
and `User`, which has permissions defined by its `Role` (one or more). You can mix the
`SecureController` trait into any type of REST controller class to make available methods
for authenticating and authorizing your routes:

```scala
// `RequestType` is the type of the request belonging to `SomeFrameWorkControllerClass`
class MySecureController extends SomeFrameWorkControllerClass with SecureController[ RequestType, MyUserType ] {
    // Define a method to authenticate user and retrieve. Note: `MyUserType` must be a
    // subtype of scala-rbac `User`
    override def authenticateUser( request : RequestType ) : MyUserType = ...

    // This will look different depending on the REST framework...
    def handleGet( request : RequestType ) : FrameworkRouteAction = {
        // This will authenticate the request and allow you to handle request with
        // along with the authenticated `User` instance
        Authenticate( request : RequestType ).withUser { implicit user =>
            ...
        }
    }

    def handlePost( request : RequestType ) : FrameworkRouteAction = {
        // This will authenticate the request and secure this route against the operation
        // `DoAThing`
        Secure( request, DoAThing ).withUser { implicit user =>
            ...
        }
    }
}
```

There are also base controller classes for play and scalatra applications that simplify the
usage for their respective frameworks:

```scala
class MyServiceClass {
    // Service method is secured using Permissible
    def doAThing(name: String)(implicit ps: PermissionSource) : String = DoAThing.secure {
        s"Hello $name"
    }
}

// SCALATRA base controller
class MyScalatraServlet( service: MyServiceClass ) extends SecureScalatraServlet[ MyUserType ] {
    
    // Which header contains your Authorization information?
    override val authHeader: String = "Authorization"
    
    // Supply some method to validate header and retrieve user:
    override def authenticateUser(authHeader: String): MyUserType = ...

    // Define a route:
    get( "/hello/:name" ) (AuthenticateRoute.withUser { 
        implicit user: MyUserType => 
            val res = service.doAThing(params("name")) 
            Ok( res )
    })
}

// PLAY base controller
class MySecurePlayController @Inject()(service: MyServiceClass, cc: ControllerComponents) extends SecurePlayController[ MyUserType ](cc) {
    // Which header contains your Authorization information?
    override val authHeader : String = "Authorization"

    // Supply some method to validate header and retrieve user:
    override def authenticateUser(authHeader: String): MyUserType = ...

    // Define a route (note that your handler should accept two parameters: the request
    // and the authenticated user:
    def getRoot(): Action[ AnyContent ] = AuthenticateAction.withUser {
        (req : Request[ AnyContent ], user: MyUserType) =>
            implicit val ps = user
            val res = service.doAThing(params("name"))
            Ok( res )
    }
}
```

The SecuredController method `Authenticate.withUser` reads the request object,
parsing out the header we have specified by `authHeader`. It then calls our
`authenticateUser` method, which either retrieves a `User` if authentication
was successful or throws an exception. If a user is successfully
retrieved, `withUser` then passes it as an argument to the function
we provide as a parameter. By adding `implicit` to the function parameter
`user : User =>`, the `user` object becomes the permission source that will
enable us to call secured service methods.

In addition to authenticating against a `User`, you can also override `authenticateRole`
or `authenticatePermission` and call `Authenticate.withRole {}` or
`Authenticate.withPermission {}` to use roles and permissions respectively. Finally,
you can use `authenticate` and `Authenticate {}` simply to validate the header
without retrieving any further permissions information.

It is also possible to secure against operations directly using the
`Secure` object:

```scala
class MySecureServlet( service : MyServiceClass ) extends SecureScalatraServlet[ MyUserType ] {
    
    // Which header contains your Authorization information?
    override val authHeader : String = "Authorization"
    
    // Supply some method to validate header and retrieve user:
    override def authenticateUser(authHeader: String): MyUserType = ...

    // Define a route:
    get( "/hello/:name" ) ( Secure(DoAThing).withUser { _ =>
        s"Hello ${params("name")}"
    } )
}
```

This accomplishes the same as the previous servlet: it authenticates against
user credentials, and then only executes the router logic if the credentials
permit `DoAThing`. Note that it also passes the authenticated `User` object to
our handler. Since we don't need it in the above case (because the authorization
has already happened) we just use `_ =>` to ignore it.

