package xyz.xenondevs.nova.serialization

import com.google.gson.*
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.attachment.Attachment
import xyz.xenondevs.nova.util.GSON
import xyz.xenondevs.nova.util.fromJson
import xyz.xenondevs.nova.util.getBoolean
import java.lang.reflect.Type
import java.util.*

object AttachmentSerializer : JsonSerializer<Attachment> {

    override fun serialize(src: Attachment, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.add("uuid", GSON.toJsonTree(src.uuid))
        jsonObject.add("itemStack", GSON.toJsonTree(src.itemStack))
        jsonObject.addProperty("hideOnDown", src.hideOnDown)

        return jsonObject
    }

}

object AttachmentDeserializer : JsonDeserializer<Attachment> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Attachment {
        json as JsonObject
        val uuid: UUID = GSON.fromJson(json.get("uuid"))!!
        val itemStack: ItemStack = GSON.fromJson(json.get("itemStack"))!!
        val hideOnDown: Boolean = json.getBoolean("hideOnDown")
        
        return Attachment(uuid, itemStack, hideOnDown)
    }

}