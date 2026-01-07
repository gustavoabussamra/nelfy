import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { AuthProvider } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import AdminRoute from './components/AdminRoute';
import UserRoute from './components/UserRoute';
import HomeRedirect from './components/HomeRedirect';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Transactions from './pages/Transactions';
import Categories from './pages/Categories';
import Profile from './pages/Profile';
import Admin from './pages/Admin';
import AdminWithdrawals from './pages/AdminWithdrawals';
import Budgets from './pages/Budgets';
import Goals from './pages/Goals';
import Reports from './pages/Reports';
import Installments from './pages/Installments';
import Referrals from './pages/Referrals';
import Accounts from './pages/Accounts';
import RecurringTransactions from './pages/RecurringTransactions';
import AutomationRules from './pages/AutomationRules';
import Layout from './components/Layout';
import './App.css';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route
            path="/"
            element={
              <UserRoute>
                <Layout>
                  <Dashboard />
                </Layout>
              </UserRoute>
            }
          />
          <Route
            path="/transactions"
            element={
              <UserRoute>
                <Layout>
                  <Transactions />
                </Layout>
              </UserRoute>
            }
          />
          <Route
            path="/categories"
            element={
              <UserRoute>
                <Layout>
                  <Categories />
                </Layout>
              </UserRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <PrivateRoute>
                <Layout>
                  <Profile />
                </Layout>
              </PrivateRoute>
            }
          />
          <Route
            path="/budgets"
            element={
              <UserRoute>
                <Layout>
                  <Budgets />
                </Layout>
              </UserRoute>
            }
          />
          <Route
            path="/goals"
            element={
              <UserRoute>
                <Layout>
                  <Goals />
                </Layout>
              </UserRoute>
            }
          />
          <Route
            path="/reports"
            element={
              <UserRoute>
                <Layout>
                  <Reports />
                </Layout>
              </UserRoute>
            }
          />
          <Route
            path="/installments"
            element={
              <UserRoute>
                <Layout>
                  <Installments />
                </Layout>
              </UserRoute>
            }
          />
          <Route
            path="/referrals"
            element={
              <UserRoute>
                <Layout>
                  <Referrals />
                </Layout>
              </UserRoute>
            }
          />
          <Route
            path="/accounts"
            element={
              <UserRoute>
                <Layout>
                  <Accounts />
                </Layout>
              </UserRoute>
            }
          />
          <Route
            path="/recurring"
            element={
              <UserRoute>
                <Layout>
                  <RecurringTransactions />
                </Layout>
              </UserRoute>
            }
          />
          <Route
            path="/automation"
            element={
              <UserRoute>
                <Layout>
                  <AutomationRules />
                </Layout>
              </UserRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <AdminRoute>
                <Layout>
                  <Admin />
                </Layout>
              </AdminRoute>
            }
          />
          <Route
            path="/admin/withdrawals"
            element={
              <AdminRoute>
                <Layout>
                  <AdminWithdrawals />
                </Layout>
              </AdminRoute>
            }
          />
          <Route path="*" element={<HomeRedirect />} />
        </Routes>
        <ToastContainer
          position="top-right"
          autoClose={3000}
          hideProgressBar={false}
          newestOnTop={false}
          closeOnClick
          rtl={false}
          pauseOnFocusLoss
          draggable
          pauseOnHover
        />
      </Router>
    </AuthProvider>
  );
}

export default App;

