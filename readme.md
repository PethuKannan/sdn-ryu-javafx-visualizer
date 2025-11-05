# SDN Controller API Integration
### Java-Based Client for Ryu Controller with Topology Visualization

This project demonstrates **Software-Defined Networking (SDN)** using the **Ryu Controller**,
**Mininet**, and a **Java/JavaFX Client** that interacts with the controller’s REST API.

---

## 🧠 Features
- Fetch and display topology (switches, links, hosts)
- Add/Delete OpenFlow flow rules via REST API
- Real-time topology visualization in JavaFX
- One-click demo launcher (`run_sdn_demo.sh`)

---

## 🧰 Requirements
- Ubuntu 20.04 LTS  
- Java 11 +, Maven  
- Python 3, Ryu, Mininet  
- tmux (optional for the demo script)  
- jq (optional for pretty JSON)

---

## ⚙️ How to Run
```bash
cd SDNClient
chmod +x run_sdn_demo.sh
./run_sdn_demo.sh
