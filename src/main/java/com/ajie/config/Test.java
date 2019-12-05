package com.ajie.config;

public class Test {
	private String name;
	private int age;

	public Test(String name, int age) {
		this.name = name;
		this.age = age;
	}

	public Test() {

	}

	
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String toString() {
		return "name:" + name + ",age:" + age;
	}
}
