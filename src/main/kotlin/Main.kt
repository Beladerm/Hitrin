
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.spec.InteractionReplyEditSpec
import discord4j.core.spec.MessageCreateSpec
import discord4j.discordjson.json.ApplicationCommandData
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.TimeoutException


fun main() {
    val token = Config.token
    val client = DiscordClient.create(token)

    val gateway = client.login().block()

    gateway.on(MessageCreateEvent::class.java).subscribe { event: MessageCreateEvent ->
        val message = event.message
        if ("!ping" == message.content) {
            val channel = message.channel.block()
            channel.createMessage("Pong!").block()
        }
    }

    //
    val applicationId = client.applicationId.block()
    val greetCmdRequest = ApplicationCommandRequest.builder()
        .name("greet")
        .description("Greet someone")
        .addOption(
            ApplicationCommandOptionData.builder()
            .name("name")
            .description("Your name")
            .type(ApplicationCommandOption.Type.STRING.value)
            .required(true)
            .build())
        .build()

    val pingCmdRequest = ApplicationCommandRequest.builder()
        .name("ping")
        .description("Ping someone")
        .build()
    //
//    client.applicationService
//        .createGlobalApplicationCommand(applicationId, greetCmdRequest)
//        .subscribe()
    //

    //
    val guildId = 952166341790539836L
    client.applicationService
        .createGuildApplicationCommand(applicationId, guildId, greetCmdRequest)
        .subscribe()
    //

    client.applicationService
        .createGuildApplicationCommand(applicationId, guildId, pingCmdRequest)
        .subscribe()

    val discordCommands = client
        .applicationService
        .getGuildApplicationCommands(applicationId, guildId)
        .collectMap(ApplicationCommandData::name)
        .block()

    val discordGreetCmd = discordCommands[greetCmdRequest.name()]

    client.applicationService.modifyGuildApplicationCommand(applicationId, guildId, discordGreetCmd!!.id().toLong(), greetCmdRequest)
        .subscribe()

    gateway.on(ChatInputInteractionEvent::class.java).subscribe { event ->
        if (event.commandName == "greet") {
            val name = event.getOption("name").get().value.get().asString()
            event.reply("Hello, $name!").block()

            event.createFollowup("Gay Сергей").subscribe()

            event.editReply(InteractionReplyEditSpec.create()
                .withContentOrNull("Blyat!")
            ).subscribe()
        }
    }



//    val channelId = Snowflake.of(952178165617426482)
//    val button = Button.success("custom-id", "Сергей!")
//
//    gateway.getChannelById(channelId)
//        .ofType(GuildMessageChannel::class.java)
//        .flatMap { channel ->
//            val createMessage = channel.createMessage(MessageCreateSpec.builder()
//            .addComponent(ActionRow.of(button))
//            .build()
//            )
//
//        )}.subscribe()

    gateway.on(ButtonInteractionEvent::class.java).subscribe {event ->
        if (event.customId.equals("custom-id")) {
            event.reply("GAAAAY!").withEphemeral(true)
        } else Mono.empty();
    }

// Whatever channel you want the message in
    // Whatever channel you want the message in
    val channelId = Snowflake.of(0)

    val button = Button.primary("custom-id", "Сергей!")

    gateway.on(MessageCreateEvent::class.java).subscribe {context ->
        if (context.message.content.equals("!")) {
            val channel  =context.message.channelId
            gateway.getChannelById(channel)
                .ofType(GuildMessageChannel::class.java)
                .flatMap { channel ->
                    val createMessageMono: Mono<Message> = channel.createMessage(
                        MessageCreateSpec.builder()
                            .addComponent(ActionRow.of(button))
                            .build()
                    )
                    val tempListener: Mono<Void> = gateway.on(ButtonInteractionEvent::class.java) { event ->
                        if (event.getCustomId().equals("custom-id")) {
                            return@on event.reply("GAAAAY!") //.withEphemeral(true)
                        } else {
                            // Ignore it
                            return@on Mono.empty()
                        }
                    }.timeout(Duration.ofMinutes(30)) // Timeout after 30 minutes
                        // Handle TimeoutException that will be thrown when the above times out
                        .onErrorResume(TimeoutException::class.java) { ignore -> Mono.empty() }
                        .then() //Transform the flux to a mono
                    createMessageMono.then(tempListener)
                }.subscribe()
        }
    }
    gateway.getChannelById(channelId)
        .ofType(GuildMessageChannel::class.java)
        .flatMap { channel ->
            val createMessageMono: Mono<Message> = channel.createMessage(
                MessageCreateSpec.builder()
                    .addComponent(ActionRow.of(button))
                    .build()
            )
            val tempListener: Mono<Void> = gateway.on(ButtonInteractionEvent::class.java) { event ->
                if (event.getCustomId().equals("custom-id")) {
                    return@on event.reply("GAAAAY!") //.withEphemeral(true)
                } else {
                    // Ignore it
                    return@on Mono.empty()
                }
            }.timeout(Duration.ofMinutes(30)) // Timeout after 30 minutes
                // Handle TimeoutException that will be thrown when the above times out
                .onErrorResume(TimeoutException::class.java) { ignore -> Mono.empty() }
                .then() //Transform the flux to a mono
            createMessageMono.then(tempListener)
        }.subscribe()

    gateway.onDisconnect().block()
}