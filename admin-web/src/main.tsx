import React from 'react';
import { createRoot } from 'react-dom/client';
import './style.css';

function App() {
  return <main className="shell">
    <header><div className="brand">FixNear <span>ADMIN</span></div><div className="tag">Panel administrativo · Starter</div></header>
    <section className="hero"><p className="eyebrow">CENTRO DE CONTROL</p><h1>Administración de la plataforma</h1><p>Base inicial para supervisar vacantes, empresas, usuarios, suscripciones y reportes.</p></section>
    <section className="grid">
      {[
        ['Usuarios','Consulta y revisión de cuentas','01'],['Empresas','Verificación y miembros','02'],['Vacantes','Moderación de publicaciones','03'],['Suscripciones','Planes y estados del proveedor','04'],['Pagos','Consulta de transacciones verificadas','05'],['Reportes','Denuncias y seguimiento','06']
      ].map(([title,desc,n])=><article key={n}><small>{n}</small><h2>{title}</h2><p>{desc}</p><b>Preparación de módulo</b></article>)}
    </section>
    <footer>Panel inicial. No conectado a datos reales. Configura autenticación administrativa, RLS y Edge Functions antes de habilitar operaciones.</footer>
  </main>
}

createRoot(document.getElementById('root')!).render(<React.StrictMode><App /></React.StrictMode>);
