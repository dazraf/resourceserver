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
    BASE_DIR.mkdir()

    vertx.createHttpServer()
        .requestHandler { router.accept(it) }
        .listen(8080)
    LOG.info("started")
    startFuture.complete()
  }

  fun RoutingContext.postHandler() {
    val file = File(BASE_DIR, this.request().path())

    val contents = this.body
    vertx.fileSystem().writeFile(file.canonicalPath, contents) {
      response().setStatusCode(200).setStatusMessage("uploaded").end()
    }
  }
}
