/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtclinicmanagementsystem;

public class Medicine {
    private String name;
    private int quantity;
    private String unit;
    private String usage;

    public Medicine(String name, int quantity, String unit, String usage) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.usage = usage;
    }

    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public String getUsage() { return usage; }

    public void setQuantity(int quantity) { this.quantity = quantity; }

    @Override
    public String toString() {
        return String.format("%-20s %-10d %-10s %s", name, quantity, unit, usage);
    }
}

