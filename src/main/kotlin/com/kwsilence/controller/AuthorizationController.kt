package com.kwsilence.controller

import com.kwsilence.mserver.BuildConfig
import com.kwsilence.service.LoginService
import com.kwsilence.service.RegistrationService
import com.kwsilence.service.ResetPasswordService
import com.kwsilence.util.ApiHelper
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveOrNull
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Routing.setupAuthorizationController(
    registrationService: RegistrationService,
    loginService: LoginService,
    resetPasswordService: ResetPasswordService
) {
    post("/register") {
        registrationService.register(call.receiveOrNull())
        call.respond(HttpStatusCode.OK, "Registration successful")
    }

    post("/login") {
        val tokenPair = loginService.login(call.receiveOrNull(), BuildConfig.useConfirm)
        call.respondText(ContentType.Application.Json, HttpStatusCode.OK) {
            Json.encodeToString(tokenPair)
        }
    }

    post(ApiHelper.RESET_PASS_PATH) {
        val mail = call.receiveOrNull<Map<String, String>>()?.get("mail")
        resetPasswordService.sendResetPasswordMail(mail, BuildConfig.useConfirm)
        call.respond(HttpStatusCode.OK, "password reset link sent")
    }

    get("${ApiHelper.RESET_PASS_PATH}/{token}") {
        resetPasswordService.findUserId(call.parameters["token"])
        call.respondText(resetHtml, ContentType.Text.Html, HttpStatusCode.OK)
    }

    post("${ApiHelper.RESET_PASS_PATH}/{token}") {
        resetPasswordService.resetPassword(call.parameters["token"], call.receiveOrNull())
        call.respond(HttpStatusCode.OK, "password successfully changed")
    }

    post("/refresh") {
        val refreshToken = call.receiveOrNull<Map<String, String>>()?.get("refreshToken")
        val tokenPair = loginService.refreshToken(refreshToken)
        call.respondText(ContentType.Application.Json, HttpStatusCode.OK) {
            Json.encodeToString(tokenPair)
        }
    }

    post(ApiHelper.CONFIRM_PATH) {
        val mail = call.receiveOrNull<Map<String, String>>()?.get("mail")
        registrationService.sendConfirmMessage(mail)
        call.respond(HttpStatusCode.OK, "confirm mail link sent")
    }

    get("${ApiHelper.CONFIRM_PATH}/{token}") {
        registrationService.confirmMail(call.parameters["token"])
        call.respond(HttpStatusCode.OK, "mail was successfully confirmed")
    }
}

private const val resetHtml = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="utf-8">
        <title>Change Password</title>
    </head>
    <body>
    <div id="result" style="visibility: hidden"></div>
    <div id="form">
        <form class="pure-form">
            <fieldset>
                <legend>Change Password</legend>
    
                <label for="password">New password: </label>
                <input type="password" id="password" required>
                <input type="button" onclick="passVisibility()" id="vPassword" value="x">

                <input type="button" onclick="return validatePassword()" id="submit" value="Confirm">
            </fieldset>
        </form>
    </div>
    <script>

        function passVisibility() {
            let password = document.getElementById("password")
            let vPass = document.getElementById("vPassword")
            if (vPass.value === "x") {
                password.type = "text"
                vPass.value = "v"
            } else {
                password.type = "password"
                vPass.value = "x"
            }
        }

        function validatePassword() {
            let password = document.getElementById("password")

            let form = document.getElementById("form")
            let result = document.getElementById("result")
            document.getElementById("password")
            let xhr = new XMLHttpRequest();
            xhr.open("POST", window.location.href);
            xhr.setRequestHeader("Accept", "application/json");
            xhr.setRequestHeader("Content-Type", "application/json");

            xhr.onreadystatechange = function () {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        form.style.visibility = "hidden"
                        result.innerText = xhr.responseText
                        result.style.visibility = "visible"
                    } else {
                        alert(xhr.responseText)
                    }
                }
            };

            let data = "{ \"newPassword\": \"" + password.value.trim() + "\" }";
            xhr.send(data);
        }
    </script>
    </body>
    </html>
"""