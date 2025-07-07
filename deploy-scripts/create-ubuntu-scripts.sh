#!/bin/bash

echo "======================================================="
echo "  Presupuesto Backend - Ubuntu Management Scripts"
echo "======================================================="
echo ""
echo "This script will create all necessary management scripts for Ubuntu"
echo ""

# Function to check and install Java
check_and_install_java() {
    echo "Checking Java installation..."
    
    if java -version 2>&1 | grep -q "openjdk version"; then
        echo "Java is already installed:"
        java -version
        echo ""
    else
        echo "Java not found. Installing OpenJDK 17..."
        sudo apt update -y
        sudo apt install openjdk-17-jdk -y
        
        if [ $? -eq 0 ]; then
            echo "Java installed successfully:"
            java -version
            echo ""
        else
            echo "ERROR: Failed to install Java"
            exit 1
        fi
    fi
}

# Check and install Java first
check_and_install_java

# Create scripts directory
mkdir -p ~/presupuesto-scripts
cd ~/presupuesto-scripts

echo "Creating management scripts..."

# 1. Deploy script
cat > deploy-presupuesto.sh << 'EOF'
#!/bin/bash
echo "======================================================="
echo "  Deploying Presupuesto Backend"
echo "======================================================="
echo ""

APP_DIR="$HOME/presupuesto-app"
SERVICE_NAME="presupuesto"

echo "Step 1: Stopping existing service..."
sudo systemctl stop $SERVICE_NAME 2>/dev/null || echo "Service was not running"

echo ""
echo "Step 2: Setting up application directory..."
mkdir -p $APP_DIR

echo ""
echo "Step 3: Installing Java and dependencies..."
sudo apt update -y
sudo apt install openjdk-17-jdk curl -y

echo ""
echo "Step 4: Configuring firewall..."
sudo ufw allow 22
sudo ufw allow 8443
sudo ufw allow 80
sudo ufw allow 443
sudo ufw --force enable

echo ""
echo "Step 5: Creating systemd service..."
sudo tee /etc/systemd/system/$SERVICE_NAME.service > /dev/null << 'SEOF'
[Unit]
Description=Presupuesto Backend Service
After=network.target

[Service]
Type=simple
User=ubuntu
Group=ubuntu
WorkingDirectory=/home/ubuntu/presupuesto-app
ExecStart=/usr/bin/java -jar /home/ubuntu/presupuesto-app/presupuesto-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=presupuesto
Environment=SERVER_PORT=8080
Environment=SERVER_PORT=8443
Environment=AWS_REGION=us-east-1

[Install]
WantedBy=multi-user.target
SEOF

echo ""
echo "Step 6: Enabling and starting service..."
sudo systemctl daemon-reload
sudo systemctl enable $SERVICE_NAME

if [ -f "$APP_DIR/presupuesto-backend-1.0.0-SNAPSHOT.jar" ]; then
    echo "Starting service..."
    sudo systemctl start $SERVICE_NAME
    sleep 10
    
    echo ""
    echo "Checking service status..."
    sudo systemctl status $SERVICE_NAME --no-pager
    
    echo ""
    echo "Testing health endpoint..."
    curl -k https://localhost:8443/health || echo "Health endpoint not ready yet"
else
    echo "JAR file not found. Please upload the JAR file to $APP_DIR first."
    echo "Then run: sudo systemctl start $SERVICE_NAME"
fi

echo ""
echo "Deployment completed!"
echo "Upload your JAR file to: $APP_DIR/"
echo "Then restart with: sudo systemctl restart $SERVICE_NAME"
EOF

# 2. Status check script
cat > check-status.sh << 'EOF'
#!/bin/bash
echo "======================================================="
echo "  Presupuesto Backend - Status Check"
echo "======================================================="
echo ""

SERVICE_NAME="presupuesto"

echo "System Information:"
echo "- Server: $(hostname)"
echo "- Date: $(date)"
echo "- Uptime: $(uptime -p)"
echo ""

echo "Service Status:"
if sudo systemctl is-active --quiet $SERVICE_NAME; then
    echo "✅ Service is RUNNING"
else
    echo "❌ Service is NOT RUNNING"
fi

echo ""
echo "Detailed Service Status:"
sudo systemctl status $SERVICE_NAME --no-pager

echo ""
echo "Recent Logs (last 20 lines):"
sudo journalctl -u $SERVICE_NAME -n 20 --no-pager

echo ""
echo "Health Check:"
curl -k https://localhost:8443/health && echo "" || echo "❌ Health endpoint not responding"


echo ""
echo "API Endpoints Test:"
echo "- Health: https://$(curl -s ifconfig.me):8443/health"
echo "- Swagger: https://$(curl -s ifconfig.me):8443/swagger-ui.html"
echo "- Okta Config: https://$(curl -s ifconfig.me):8443/api/okta-config"
EOF

# 3. Logs viewer script
cat > view-logs.sh << 'EOF'
#!/bin/bash
echo "======================================================="
echo "  Presupuesto Backend - Live Logs"
echo "======================================================="
echo ""
echo "Showing live logs for presupuesto service..."
echo "Press Ctrl+C to exit"
echo ""

sudo journalctl -u presupuesto -f --no-pager
EOF

# 4. Restart script
cat > restart-service.sh << 'EOF'
#!/bin/bash
echo "======================================================="
echo "  Presupuesto Backend - Restart Service"
echo "======================================================="
echo ""

SERVICE_NAME="presupuesto"

echo "Restarting $SERVICE_NAME service..."
sudo systemctl restart $SERVICE_NAME

echo "Waiting for service to start..."
sleep 10

echo ""
echo "Service Status:"
sudo systemctl status $SERVICE_NAME --no-pager

echo ""
echo "Testing health endpoint..."
curl -s http://localhost:8080/health && echo " - Health check OK" || echo " - Health check failed"
curl -k https://localhost:8443/health && echo " - Health check OK" || echo " - Health check failed"

echo ""
echo "Recent logs:"
sudo journalctl -u $SERVICE_NAME -n 10 --no-pager
EOF

# 5. Update application script
cat > update-app.sh << 'EOF'
#!/bin/bash
echo "======================================================="
echo "  Presupuesto Backend - Update Application"
echo "======================================================="
echo ""

APP_DIR="$HOME/presupuesto-app"
SERVICE_NAME="presupuesto"
JAR_FILE="$APP_DIR/presupuesto-backend-1.0.0-SNAPSHOT.jar"

echo "Updating application using: $JAR_FILE"
echo ""

echo "Step 1: Stopping service..."
sudo systemctl stop $SERVICE_NAME

echo ""
echo "Step 2: Backing up current JAR..."
if [ -f "$JAR_FILE" ]; then
    cp "$JAR_FILE" "$APP_DIR/presupuesto-backend-backup-$(date +%Y%m%d-%H%M%S).jar"
    echo "Backup created"
fi

echo ""
echo "Step 3: (Re)copy your new JAR to $JAR_FILE before running this script!"
echo ""

echo "Step 4: Starting service..."
sudo systemctl start $SERVICE_NAME

echo ""
echo "Step 5: Waiting for service to start..."
sleep 15

echo ""
echo "Step 6: Checking service status..."
sudo systemctl status $SERVICE_NAME --no-pager

echo ""
echo "Step 7: Testing health endpoint..."
curl -k https://localhost:8443/health && echo " - Update successful!" || echo " - Health check failed"

echo ""
echo "Recent logs:"
sudo journalctl -u $SERVICE_NAME -n 15 --no-pager
EOF


# 7. Environment info script
cat > system-info.sh << 'EOF'
#!/bin/bash
echo "======================================================="
echo "  System Information"
echo "======================================================="
echo ""

echo "Server Details:"
echo "- Hostname: $(hostname)"
echo "- Public IP: $(curl -s ifconfig.me)"
echo "- Private IP: $(hostname -I | awk '{print $1}')"
echo "- OS: $(lsb_release -d | cut -f2)"
echo "- Kernel: $(uname -r)"
echo "- Uptime: $(uptime -p)"
echo ""

echo "Java Information:"
java -version 2>&1 | head -3
echo ""

echo "Disk Usage:"
df -h / | tail -1
echo ""

echo "Memory Usage:"
free -h | grep -E "(Mem|Swap)"
echo ""

echo "Network Ports:"
echo "Active ports:"
sudo netstat -tlnp | grep -E ":(80|443|8080)"
sudo netstat -tlnp | grep -E ":(80|443|8080|8443)"
echo ""

echo "Firewall Status:"
sudo ufw status
echo ""

echo "Services Status:"
echo "- Presupuesto: $(sudo systemctl is-active presupuesto 2>/dev/null || echo 'not installed')"

echo ""

echo "Application Files:"
ls -la ~/presupuesto-app/ 2>/dev/null || echo "Application directory not found"
EOF

# 7. Java verification and installation script
cat > check-install-java.sh << 'EOF'
#!/bin/bash
echo "======================================================="
echo "  Java Verification and Installation"
echo "======================================================="
echo ""

check_java() {
    echo "Checking Java installation..."
    
    if command -v java &> /dev/null; then
        echo "✅ Java is installed:"
        java -version
        echo ""
        
        if command -v javac &> /dev/null; then
            echo "✅ Java Development Kit (JDK) is available:"
            javac -version
        else
            echo "⚠️  Java Development Kit (JDK) is NOT installed"
            echo "   Only Java Runtime Environment (JRE) is available"
        fi
        
        echo ""
        echo "Java Installation Details:"
        echo "- Java executable: $(which java)"
        echo "- Java version: $(java -version 2>&1 | head -1)"
        
        if [ -n "$JAVA_HOME" ]; then
            echo "- JAVA_HOME: $JAVA_HOME"
        else
            echo "- JAVA_HOME: Not set"
        fi
        
        return 0
    else
        echo "❌ Java is NOT installed"
        return 1
    fi
}

install_java() {
    echo ""
    echo "Installing OpenJDK 17..."
    echo "Updating package repositories..."
    sudo apt update -y
    
    echo "Installing OpenJDK 17 JDK..."
    sudo apt install openjdk-17-jdk -y
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "✅ Java installed successfully!"
        echo "Verifying installation..."
        java -version
        javac -version
        
        echo ""
        echo "Setting JAVA_HOME..."
        JAVA_PATH=$(readlink -f /usr/bin/java | sed "s:bin/java::")
        echo "export JAVA_HOME=$JAVA_PATH" >> ~/.bashrc
        export JAVA_HOME=$JAVA_PATH
        
        echo "✅ JAVA_HOME set to: $JAVA_HOME"
        echo "✅ Java installation completed successfully!"
        
        return 0
    else
        echo "❌ Failed to install Java"
        return 1
    fi
}

# Main execution
if ! check_java; then
    echo ""
    read -p "Do you want to install Java now? (y/n): " -n 1 -r
    echo ""
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        install_java
    else
        echo "Java installation skipped."
        echo "Note: Java is required to run the Presupuesto Backend application."
    fi
else
    echo "Java verification completed!"
fi

echo ""
echo "Done!"
EOF

# Make all scripts executable
chmod +x *.sh

echo ""
echo "✅ All scripts created successfully in ~/presupuesto-scripts/"
echo ""
echo "Available scripts:"
echo "- deploy-presupuesto.sh    : Deploy/setup the application"
echo "- check-status.sh          : Check service status and health"
echo "- view-logs.sh            : View live application logs"
echo "- restart-service.sh      : Restart the application service"
echo "- update-app.sh           : Update application JAR file"
echo "- system-info.sh          : Show system information"
echo "- check-install-java.sh   : Verify and install Java if needed"
echo ""
echo "To use a script, run: ./script-name.sh"
echo "Example: ./check-status.sh"
echo "Example: ./check-install-java.sh"
