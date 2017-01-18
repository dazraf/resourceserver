package io.dazraf.resourceserver

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.shiro.ShiroAuth
import io.vertx.ext.auth.shiro.ShiroAuthOptions
import io.vertx.ext.auth.shiro.ShiroAuthRealmType
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.sstore.LocalSessionStore
import org.slf4j.LoggerFactory.getLogger
import java.io.File


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

    router.route().handler(CookieHandler.create())
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)))
    val config = JsonObject().put("properties_path", "classpath:vertx-users.properties")

    val provider = ShiroAuth.create(vertx, ShiroAuthOptions().setType(ShiroAuthRealmType.PROPERTIES).setConfig(config))
    router.route().handler(UserSessionHandler.create(provider))

    val basicAuthHandler = BasicAuthHandler.create(provider)

    router.route().handler(BodyHandler.create())
    router.post("/*").handler { basicAuthHandler.handle(it) }

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
    file.parentFile.canonicalFile.mkdir()
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

