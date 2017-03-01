package cn.com.aboobear.spam;

public class Rule {
	private int id;
	private String name;
	private String condition;
	private int actions;
	private String forward;
	private int fowardanddeliver;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCondition() {
		return condition;
	}
	public void setCondition(String condition) {
		this.condition = condition;
	}
	public int getActions() {
		return actions;
	}
	public void setActions(int actions) {
		this.actions = actions;
	}
	public String getForward() {
		return forward;
	}
	public void setForward(String forware) {
		this.forward = forware;
	}
	public int getFowardanddeliver() {
		return fowardanddeliver;
	}
	public void setFowareanddeliver(int fowardanddeliver) {
		this.fowardanddeliver = fowardanddeliver;
	}
}
