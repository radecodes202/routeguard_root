# RouteGuard Admin Dashboard

This is the administrative web dashboard for RouteGuard, built with React Vite.

## Features

- Advisory management (Create, Read, Update, Delete)
- Responsive design
- Real-time updates (placeholder for WebSocket integration)

## Getting Started

### Prerequisites

- Node.js (v14 or higher)
- npm or yarn

### Installation

1. Clone the repository
2. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
3. Install dependencies:
   ```bash
   npm install
   ```

### Configuration

Create a `.env` file in the frontend directory with the following content:

```
VITE_API_URL=http://localhost:3000/api/v1
```

Adjust the API URL if your backend is running on a different port or host.

### Development Server

To start the development server:

```bash
npm run dev
```

The application will be available at http://localhost:3001

### Production Build

To create a production build:

```bash
npm run build
```

To preview the production build:

```bash
npm run preview
```

## Project Structure

```
frontend/
├── public/
├── src/
│   ├── components/
│   ├── pages/
│   │   └── advisories/
│   │       └── AdvisoriesPage.jsx
│   ├── App.jsx
│   ├── main.jsx
│   └── index.css
├── .env
├── index.html
├── package.json
�└── vite.config.js
```

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm audit` - Check for security vulnerabilities

## Technology Stack

- React 18
- Vite
- React Router DOM
- TanStack Query (React Query)
- React Hook Form
- Zod (for validation)
- Tailwind CSS
- Leaflet (for maps)
- React Icons

## Backend Integration

This frontend expects the RouteGuard backend to be running and accessible at the API URL specified in the `.env` file. The backend should provide the following endpoints for advisory management:

- GET `/api/v1/advisories` - Get all advisories
- POST `/api/v1/advisories` - Create a new advisory
- GET `/api/v1/advisories/:id` - Get advisory by ID
- PUT `/api/v1/advisories/:id` - Update advisory
- DELETE `/api/v1/advisories/:id` - Delete advisory
- GET `/api/v1/advisories/active` - Get active advisories

Make sure the backend is running and these endpoints are implemented before using the frontend.