package com.infogen.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * @author larry
 * @version 创建时间 2017年9月26日 上午11:18:48
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, setterVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE)
public class NOTIN extends Operator {
	private static final long serialVersionUID = 103243415527557652L;

	public NOTIN(String key, Set<? extends Object> items) {
		super();
		this.key = key;
		for (Object string : items) {
			this.items.add(string.toString());
		}
	}

	private List<String> items = new ArrayList<>();
	public String key = "";

	public void add(String item) {
		items.add(item);
	}

	public String to_filter() {
		if (items.isEmpty()) {
			return " 1 = 1 ";
		}
		StringBuilder string_builder = new StringBuilder();
		string_builder.append(" ").append(key).append(" NOT IN ");
		string_builder.append("(");
		for (int i = 0; i < items.size(); i++) {
			string_builder.append("'");
			string_builder.append(items.get(i));
			string_builder.append("'");
			if (i != items.size() - 1) {
				string_builder.append(" , ");
			}
		}
		string_builder.append(")");
		return string_builder.toString();
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @param key
	 *            the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * @return the items
	 */
	public List<String> getItems() {
		return items;
	}

	/**
	 * @param items
	 *            the items to set
	 */
	public void setItems(List<String> items) {
		this.items = items;
	}

}