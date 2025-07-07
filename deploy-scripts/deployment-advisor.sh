#!/bin/bash

# Script de decisión para deployment: EC2 vs Lambda
# Este script ayuda a decidir qué tipo de deployment usar

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  PRESUPUESTO BACKEND - DEPLOYMENT ADVISOR${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

echo -e "${YELLOW}Este script te ayudará a decidir entre EC2 y Lambda${NC}"
echo ""

# Función para hacer preguntas
ask_question() {
    local question="$1"
    local default="$2"
    echo -e "${GREEN}$question${NC}"
    if [ -n "$default" ]; then
        echo -e "${YELLOW}(Por defecto: $default)${NC}"
    fi
    read -r answer
    if [ -z "$answer" ] && [ -n "$default" ]; then
        answer="$default"
    fi
    echo "$answer"
}

# Variables para puntuación
ec2_score=0
lambda_score=0

echo -e "${YELLOW}Responde las siguientes preguntas:${NC}"
echo ""

# Pregunta 1: Patrón de tráfico
echo "1. ¿Cuál es el patrón de tráfico esperado?"
echo "   a) Constante durante horas de trabajo"
echo "   b) Esporádico/Variable"
echo "   c) Picos ocasionales"
traffic_pattern=$(ask_question "Respuesta (a/b/c):" "b")

case $traffic_pattern in
    a) ec2_score=$((ec2_score + 2)) ;;
    b) lambda_score=$((lambda_score + 2)) ;;
    c) lambda_score=$((lambda_score + 1)) ;;
esac

# Pregunta 2: Presupuesto
echo ""
echo "2. ¿Cuál es tu prioridad de costos?"
echo "   a) Costo fijo predecible"
echo "   b) Pagar solo por uso"
echo "   c) Minimizar costos operacionales"
budget_priority=$(ask_question "Respuesta (a/b/c):" "b")

case $budget_priority in
    a) ec2_score=$((ec2_score + 1)) ;;
    b) lambda_score=$((lambda_score + 2)) ;;
    c) lambda_score=$((lambda_score + 2)) ;;
esac

# Pregunta 3: Administración
echo ""
echo "3. ¿Qué prefieres en cuanto a administración?"
echo "   a) Control total del servidor"
echo "   b) Sin administración de infraestructura"
echo "   c) Configuración mínima"
admin_preference=$(ask_question "Respuesta (a/b/c):" "b")

case $admin_preference in
    a) ec2_score=$((ec2_score + 2)) ;;
    b) lambda_score=$((lambda_score + 2)) ;;
    c) lambda_score=$((lambda_score + 1)) ;;
esac

# Pregunta 4: Cold start
echo ""
echo "4. ¿Qué tan crítico es el tiempo de respuesta inicial?"
echo "   a) Muy crítico (< 1 segundo)"
echo "   b) Moderado (< 5 segundos es aceptable)"
echo "   c) No es crítico"
cold_start=$(ask_question "Respuesta (a/b/c):" "b")

case $cold_start in
    a) ec2_score=$((ec2_score + 2)) ;;
    b) lambda_score=$((lambda_score + 1)) ;;
    c) lambda_score=$((lambda_score + 2)) ;;
esac

# Pregunta 5: Escalabilidad
echo ""
echo "5. ¿Qué tipo de escalabilidad necesitas?"
echo "   a) Manual/Predecible"
echo "   b) Automática según demanda"
echo "   c) No necesito escalabilidad"
scalability=$(ask_question "Respuesta (a/b/c):" "b")

case $scalability in
    a) ec2_score=$((ec2_score + 1)) ;;
    b) lambda_score=$((lambda_score + 2)) ;;
    c) ec2_score=$((ec2_score + 1)) ;;
esac

echo ""
echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}              RESULTADO${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

echo -e "${YELLOW}Puntuación:${NC}"
echo -e "EC2: $ec2_score puntos"
echo -e "Lambda: $lambda_score puntos"
echo ""

if [ $lambda_score -gt $ec2_score ]; then
    echo -e "${GREEN}🚀 RECOMENDACIÓN: AWS LAMBDA${NC}"
    echo ""
    echo -e "${YELLOW}Razones:${NC}"
    echo "• Costo más eficiente para tu patrón de uso"
    echo "• Escalabilidad automática"
    echo "• Sin administración de infraestructura"
    echo "• Ideal para aplicaciones web modernas"
    echo ""
    echo -e "${YELLOW}Comandos para deployment:${NC}"
    echo -e "${GREEN}chmod +x deploy-sam.sh && ./deploy-sam.sh${NC}"
    echo "o"
    echo -e "${GREEN}deploy-sam.bat${NC} (Windows)"
    
elif [ $ec2_score -gt $lambda_score ]; then
    echo -e "${GREEN}🖥️  RECOMENDACIÓN: EC2${NC}"
    echo ""
    echo -e "${YELLOW}Razones:${NC}"
    echo "• Control total del servidor"
    echo "• Sin cold start delays"
    echo "• Ideal para tráfico constante"
    echo "• Configuración tradicional"
    echo ""
    echo -e "${YELLOW}Comandos para deployment:${NC}"
    echo -e "${GREEN}mvn clean package${NC}"
    echo -e "${GREEN}java -jar target/presupuesto-backend-1.0.0-SNAPSHOT.jar${NC}"
    
else
    echo -e "${YELLOW}⚖️  EMPATE - CUALQUIERA FUNCIONARÁ${NC}"
    echo ""
    echo -e "${YELLOW}Sugerencia:${NC}"
    echo "Comienza con Lambda para menor complejidad inicial"
    echo "Puedes migrar a EC2 más tarde si es necesario"
    echo ""
    echo -e "${YELLOW}Comando recomendado:${NC}"
    echo -e "${GREEN}chmod +x deploy-sam.sh && ./deploy-sam.sh${NC}"
fi

echo ""
echo -e "${BLUE}================================================${NC}"
echo -e "${YELLOW}¿Quieres proceder con el deployment recomendado? (y/n)${NC}"
read -r proceed

if [ "$proceed" = "y" ] || [ "$proceed" = "Y" ]; then
    if [ $lambda_score -ge $ec2_score ]; then
        echo -e "${GREEN}Iniciando deployment con Lambda...${NC}"
        if [ -f "deploy-sam.sh" ]; then
            chmod +x deploy-sam.sh
            ./deploy-sam.sh
        else
            echo -e "${RED}Error: deploy-sam.sh no encontrado${NC}"
        fi
    else
        echo -e "${GREEN}Iniciando deployment con EC2...${NC}"
        mvn clean package && echo -e "${GREEN}Ejecuta: java -jar target/presupuesto-backend-1.0.0-SNAPSHOT.jar${NC}"
    fi
else
    echo -e "${YELLOW}Deployment cancelado. Puedes ejecutar los comandos manualmente más tarde.${NC}"
fi

echo ""
echo -e "${GREEN}¡Gracias por usar el asesor de deployment!${NC}"
