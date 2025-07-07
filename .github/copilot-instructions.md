<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->

This is a Spring Boot Java project for a budget management system with the following characteristics:

## Project Structure
- **Backend**: Java Spring Boot application with Maven
- **Database**: AWS DynamoDB for data persistence
- **Authentication**: API Gateway Authorizer (Lambda-based)
- **Email**: Amazon SES for email notifications
- **Deployment**: AWS Lambda with API Gateway

## Key Components
- **Models**: Entity classes for SolicitudPresupuesto, Area, Departamento, Subdepartamento, Proveedor, CategoriaGasto, Solicitante
- **Repositories**: DynamoDB Enhanced Client repositories for data access
- **Services**: Business logic layer
- **Controllers**: REST API endpoints

## API Endpoints
- `/api/solicitudes-presupuesto` - Budget request management
- `/api/solicitantes` - Employee/requester management
- `/api/areas` - Area management
- `/api/subdepartamentos` - Sub-department management (filtered by areaId)
- `/api/departamentos` - Department management
- `/api/proveedores` - Supplier management
- `/api/categorias-gasto` - Expense category management
- `/api/resultados` - Results with optional numEmpleado filter
- `/api/resultados/editar-estatus` - Admin-only: Change request status and send email notifications
- `/api/emails/send` - Email sending functionality
- `/api/emails/send-budget-notification` - Send budget request notifications
- `/api/emails/send-status-notification` - Send status update notifications
- `/api/userInfo` - User information from API Gateway Authorizer
- `/api/logout` - Logout functionality
- `/api/debug/**` - Debug endpoints for testing
- `/health` - Health check endpoint

## Code Conventions
- Use Spring Boot annotations (@RestController, @Service, @Repository, etc.)
- Follow RESTful API design principles
- Use DynamoDB Enhanced Client for database operations
- Implement proper error handling and logging
- Use API Gateway Authorizer for authentication and authorization
- Implement role-based access control (Admin role for status changes)
- Use Amazon SES for email notifications with automatic integration
- Follow Java naming conventions and best practices
- Design for AWS Lambda execution (stateless, optimized for cold starts)
