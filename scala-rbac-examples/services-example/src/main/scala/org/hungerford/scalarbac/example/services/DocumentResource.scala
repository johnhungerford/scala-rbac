package org.hungerford.scalarbac.example.services

import org.hungerford.rbac.{AllResources, Operation, PermissibleResource, PermissionSource, Resource, ResourceOperation, ResourceRole, ResourceType, Role}

import java.util.Objects
import scala.util.{Failure, Success, Try}

sealed trait DocumentResource extends ResourceType

sealed trait DocumentNode extends DocumentResource with Resource[ DocumentResource ] {
    val id : String
    def withParent( newParent : DocumentResource ) : DocumentNode

    def asDoc[ T ] : Document[ T ] = this.asInstanceOf[ Document[ T ] ]
    def asDir : Directory = this.asInstanceOf[ Directory ]

    def getParentChain( prevChain : List[ DocumentResource ] = Nil ) : List[ DocumentResource ] = parent match {
        case _ : DocumentRoot => parent :: this :: prevChain
        case parentDir : Directory => parentDir.getParentChain( this :: prevChain )
        case _ => this :: prevChain
    }

    override def toString : String = this match {
        case dir : Directory => dir.toString
        case doc : Document[ _ ] => doc.toString
        case _ => super.toString
    }
}

trait DocumentCollection[ T ] extends DocumentResource { this : T =>
    private[example] val documents : scala.collection.mutable.Map[ String, DocumentNode ]

    def addNode( document : DocumentNode )( implicit permissionSource : PermissionSource ) : T = DocumentAccess( this, Write ).secure {
        this.documents( document.id ) = document.withParent( this )
        this
    }
    def addDoc[ A ]( id : String, content : A )( implicit permissionSource : PermissionSource ) : T = DocumentAccess( this, Write ).secure {
        this.documents( id ) = new Document[ A ]( id, this, content )
        this.asInstanceOf[ T ]
    }
    def addDir( id : String, documents : Document[ _ ]* )( implicit permissionSource : PermissionSource ) : T = DocumentAccess( this, Write ).secure {
        this.documents( id ) = new Directory( id, this, documents.toSet )
        this.asInstanceOf[ T ]
    }
    def + ( document : DocumentNode )( implicit permissionSource : PermissionSource ) : T = addNode( document.withParent( this ) )

    def removeNode( document : DocumentNode )( implicit permissionSource : PermissionSource ) : T = DocumentAccess( this, Write ).secure { this.documents.toSet.map( ( tup : (String, DocumentNode) ) => tup._2 ) - document; this }
    def removeNode( id : String )( implicit permissionSource : PermissionSource ) : T = removeNode( apply( id ) )
    def - ( document : DocumentNode )( implicit permissionSource : PermissionSource ) : T = removeNode( document )
    def - ( id : String )( implicit permissionSource : PermissionSource ) : T = removeNode( id )

    def apply( id : String )( implicit permissionSource : PermissionSource ) : DocumentNode = DocumentAccess( this, Read ).secure( documents.apply( id ) )
    def get( id : String )( implicit permissionSource : PermissionSource ) : Option[ DocumentNode ] = DocumentAccess( this, Read ).secure( documents.get( id ) )

    def children( implicit permissionSource : PermissionSource ) : List[ DocumentNode ] = DocumentAccess( this, Read ).secure( documents.values.toList )

    def dir( id : String )( implicit permissionSource: PermissionSource ) : Directory = apply( id ).asDir
    def dirOrAdd( id : String, documents : Document[ _ ]*  )( implicit permissionSource : PermissionSource ) : Directory = {
        Try( dir( id ) ) match {
            case Success( res ) => res
            case Failure( _ : NoSuchElementException ) =>
                addDir( id, documents : _* )
                dir( id )
            case Failure( _ : ClassCastException ) =>
                throw new Exception( s"$id is a document, not a directory" )
            case Failure( e ) => throw e
        }
    }

    def doc[ A ]( id : String )( implicit permissionSource: PermissionSource ) : Document[ A ] = apply( id ).asDoc[ A ]
    def docOrAdd[ A ]( id : String, content : A )( implicit permissionSource: PermissionSource ) : Document[ A ] = {
        Try( doc[ A ]( id ) ) match {
            case Success( res ) => res
            case Failure( _ : NoSuchElementException ) =>
                addDoc( id, content )
                doc( id )
            case Failure( _ : ClassCastException ) =>
                throw new Exception( s"$id is a document, not a directory" )
            case Failure( e ) => throw e
        }
    }

    def / ( id : String )( implicit ps : PermissionSource ) : Directory = dir( id )
    def >[ A ] ( id : String )( implicit ps : PermissionSource ) : Document[ A ] = doc[ A ]( id )
    def +/ ( id : String )( implicit ps : PermissionSource ) : Directory = dirOrAdd( id )
    def +>[ A ] ( id : String, content : A )( implicit ps : PermissionSource ) : Document[ A ] = docOrAdd[ A ]( id, content )

    override def toString : String = this.asInstanceOf[ T ].toString
}

class DocumentRoot( fromDocuments : Set[ DocumentNode ] ) extends DocumentCollection[ DocumentRoot ] {
    override private[example] val documents : scala.collection.mutable.Map[ String, DocumentNode ] =
        scala.collection.mutable.Map[ String, DocumentNode ]( fromDocuments.map( doc => doc.id -> doc.withParent( this ) ).toSeq : _* )

    override val parent : PermissibleResource = AllResources

    override def toString : String = s"DocumentRoot( ${documents.keys.mkString(", ")} )"
}

class Directory( override val id : String, override val parent : DocumentResource, fromDocuments : Set[ DocumentNode ] = Set[ DocumentNode ]() )
  extends DocumentCollection[ Directory ] with DocumentNode {
    override private[example] val documents : scala.collection.mutable.Map[ String, DocumentNode ] =
        scala.collection.mutable.Map[ String, DocumentNode ]( fromDocuments.map( doc => doc.id -> doc.withParent( this ) ).toSeq : _* )

    override def equals( that : Any ) : Boolean = that match {
        case thatCol : DocumentCollection[ _ ] => thatCol.documents == this.documents
        case _ => false
    }

    override def hashCode( ) : Int = Objects.hash( id, documents )

    def withParent( newParent : DocumentResource ) : Directory = new Directory( id, newParent, documents.toSet.map( ( tup : (String, DocumentNode) ) => tup._2 ) )

    override def toString : String = s"${getParentChain().map( {
        case r : DocumentRoot => ""
        case dn : DocumentNode => dn.id
        case other => other.toString
    } ).dropRight( 1 ).mkString( "/" )}/$id (${documents.size} children)"
}

class Document[ T ]( override val id : String, override val parent : DocumentResource, val content : T ) extends DocumentNode {
    override def equals( that : Any ) : Boolean = that match {
        case thatDoc : Document[ T ] => thatDoc.content == this.content && thatDoc.id == this.id
        case _ => false
    }

    override def hashCode( ) : Int = id.hashCode() * content.hashCode()

    def withParent( newParent : DocumentResource ) : Document[ T ] = new Document( id, newParent, content )

    override def toString : String = s"${getParentChain().map( {
        case _ : DocumentRoot => ""
        case dn : DocumentNode => dn.id
        case other => other.toString
    } ).dropRight( 1 ).mkString( "/" )}/$id:\n\t$content"
}

trait DocumentOperation extends Operation

case object Read extends DocumentOperation
case object Write extends DocumentOperation
case object Execute extends DocumentOperation

case class DocumentAccess( documentResource : DocumentResource, override val operation : DocumentOperation )
  extends ResourceOperation {
    override val resource : PermissibleResource = documentResource.asInstanceOf[ PermissibleResource ]
}

case class DocumentAccessRole( documentResource : DocumentResource, ops : Set[ Operation ] ) extends ResourceRole {
    override val resource : PermissibleResource = documentResource
    override val operations : Set[ Operation ] = ops
}

trait DocumentRole {
    val permittedOperations : Set[ Operation ]
    def apply( documentResource : DocumentResource ) : Role = {
        DocumentAccessRole( documentResource, permittedOperations )
    }
}

object Root extends DocumentRoot( Set[ DocumentNode ]() )
