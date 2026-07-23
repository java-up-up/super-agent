package org.javaup.config;

import cn.hutool.core.date.DateTime;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 配置类
 * @author: 阿星不是程序员
 **/

public class JsonCustomSerializer extends BeanSerializerModifier {

	@Override
	public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
			List<BeanPropertyWriter> beanProperties) {

		for (BeanPropertyWriter writer : beanProperties) {
			JsonSerializer<Object> js = judgeType(writer);
			if (js != null) {
				writer.assignNullSerializer(js);
			}
		}
		return beanProperties;
	}

	public JsonSerializer<Object> judgeType(BeanPropertyWriter writer) {
		JavaType javaType = writer.getType();
		Class<?> clazz = javaType.getRawClass();

		if (String.class.isAssignableFrom(clazz)) {
			return new JsonSerializer<Object>() {
				@Override
				public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
						throws IOException {
					gen.writeString("");
				}
			};
		}

		if (Number.class.isAssignableFrom(clazz)) {
			return new JsonSerializer<Object>() {
				@Override
				public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
						throws IOException {
					gen.writeString("");
				}
			};
		}

		if (Boolean.class.isAssignableFrom(clazz)) {
			return new JsonSerializer<Object>() {
				@Override
				public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
						throws IOException {
					gen.writeBoolean(false);
				}
			};
		}

		if (Date.class.isAssignableFrom(clazz)) {
			return new JsonSerializer<Object>() {
				@Override
				public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
						throws IOException {
					gen.writeString("");
				}
			};
		}

		if (clazz.equals(DateTime.class)) {
			return new JsonSerializer<Object>() {
				@Override
				public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
						throws IOException {
					gen.writeString("");
				}
			};
		}

		if (clazz.isArray() || clazz.equals(List.class) || clazz.equals(Set.class)) {
			return new JsonSerializer<Object>() {
				@Override
				public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
						throws IOException {
					gen.writeStartArray();
					gen.writeEndArray();
				}
			};
		}
		return null;
	}
}
