package io.dazraf.resourceserver

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.io.FileWriter

class ResourceServer : AbstractVerticle() {
  val BASE_DIR = File("files")

  companion object {
    val LOG = getLogger(ResourceServer::class.java)!!

    @JvmStatic
    fun main(args: Array<String>) {
      val vertx = Vertx.vertx()
      vertx.deployVerticle(ResourceServer())
    }
  }

  override fun start(startFuture: Future<Void>) {
    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    router.post("/*").handler { it.postHandler() }
    val sh = StaticHandler.create()
        .setAllowRootFileSystemAccess(true)
        .setWebRoot(BASE_DIR.canonicalPath!!)
        .setCachingEnabled(false)
        .setDirectoryListing(true)

    router.get("/*").handler { sh.handle(it) }
    router.delete("/*").handler { it.deleteHandler() }
    BASE_DIR.mkdir()

    vertx.createHttpServer()
        .requestHandler { router.accept(it) }
        .listen(8080)
    LOG.info("started")
    startFuture.complete()
  }

  fun RoutingContext.postHandler() {
    val file = File(BASE_DIR, this.request().path())
    val created = file.parentFile.canonicalFile.mkdir()
    val contents = this.body
    vertx.fileSystem().writeFile(file.canonicalPath, contents) {
      if (it.succeeded()) {
        response().setStatusCode(200).setStatusMessage("uploaded " + request().path()).end()
      } else {
        response().setStatusCode(500).setStatusMessage("failed to upload " + request().path()).end()
      }
    }
  }

  private fun RoutingContext.deleteHandler() {
    val file = File(BASE_DIR, this.request().path())
    if (file.canonicalFile.deleteRecursively()) {
      response().setStatusCode(200).setStatusMessage("deleted " + request().path()).end()
    } else {
      response().setStatusCode(404).setStatusMessage("failed to delete " + request().path()).end()
    }
  }
}

