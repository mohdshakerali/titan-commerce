
import { useCallback, useEffect, useState } from 'react'
import './App.css'

type Product = {
  id: number
  name: string
  description: string | null
  price: number
}

function App() {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Temporary placeholder until Issue #12.
  const cartCount = 0

  const loadProducts = useCallback(() => {
    setLoading(true)
    setError('')

    fetch('/api/products')
      .then((response) => {
        if (!response.ok) {
          throw new Error(`Unable to load products (${response.status})`)
        }
        return response.json() as Promise<Product[]>
      })
      .then((data) => {
        setProducts(data)
      })
      .catch((err: unknown) => {
        setError(
          err instanceof Error
            ? err.message
            : 'Something went wrong while loading products.',
        )
      })
      .finally(() => {
        setLoading(false)
      })
  }, [])

  useEffect(() => {
    let cancelled = false

    fetch('/api/products')
      .then((response) => {
        if (!response.ok) {
          throw new Error(`Unable to load products (${response.status})`)
        }
        return response.json() as Promise<Product[]>
      })
      .then((data) => {
        if (!cancelled) {
          setProducts(data)
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(
            err instanceof Error
              ? err.message
              : 'Something went wrong while loading products.',
          )
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  const formatPrice = (price: number) =>
    new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2,
    }).format(price)

  return (
    <div className="app">
      <header className="navbar">
        <a className="brand" href="/">
          TITAN<span>COMMERCE</span>
        </a>

        <nav className="nav-links">
          <a href="#catalogue">Shop</a>
          <a href="#about">About</a>
        </nav>

        <button
          className="cart-button"
          type="button"
          aria-label={`Shopping cart, ${cartCount} items`}
        >
          Cart <span>{cartCount}</span>
        </button>
      </header>

      <main>
        <section className="hero">
          <div className="hero-content">
            <p className="eyebrow">WELCOME TO TITAN COMMERCE</p>
            <h1>
              Find something
              <br />
              you&apos;ll love.
            </h1>
            <p className="hero-description">
              Discover our collection of products, thoughtfully selected
              for everyday life.
            </p>
            <a className="hero-button" href="#catalogue">
              Explore products <span>→</span>
            </a>
          </div>

          <div className="hero-decoration" aria-hidden="true">
            <div className="decoration-circle">T</div>
          </div>
        </section>

        <section className="catalogue" id="catalogue">
          <div className="section-heading">
            <div>
              <p className="eyebrow">OUR COLLECTION</p>
              <h2>Explore products</h2>
            </div>
            <p className="product-count">
              {products.length}{' '}
              {products.length === 1 ? 'product' : 'products'}
            </p>
          </div>

          {loading && (
            <div className="state-message">
              <div className="loader" />
              <p>Loading our collection...</p>
            </div>
          )}

          {!loading && error && (
            <div className="state-message error-state">
              <h3>We couldn&apos;t load the products.</h3>
              <p>{error}</p>
              <button
                className="retry-button"
                type="button"
                onClick={loadProducts}
              >
                Try again
              </button>
            </div>
          )}

          {!loading && !error && products.length === 0 && (
            <div className="state-message">
              <h3>Our collection is getting ready.</h3>
              <p>
                There are no products available right now.
                Please check back soon.
              </p>
            </div>
          )}

          {!loading && !error && products.length > 0 && (
            <div className="product-grid">
              {products.map((product) => (
                <article className="product-card" key={product.id}>
                  <div className="product-image">
                    <span>PRODUCT</span>
                    <div className="product-placeholder">T</div>
                  </div>

                  <div className="product-info">
                    <p className="product-category">TITAN COLLECTION</p>
                    <h3>{product.name}</h3>
                    <p className="product-description">
                      {product.description ||
                        'Discover this product from our collection.'}
                    </p>
                    <div className="product-footer">
                      <strong>{formatPrice(product.price)}</strong>
                      <span className="product-id">#{product.id}</span>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </main>

      <footer id="about">
        <a className="brand footer-brand" href="/">
          TITAN<span>COMMERCE</span>
        </a>
        <p>Built with purpose. Delivered with simplicity.</p>
        <p className="copyright">
          © {new Date().getFullYear()} Titan Commerce
        </p>
      </footer>
    </div>
  )
}

export default App