import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'

function SlideContent({ promo, active }) {
  return <>
    <picture>
      {promo.imagenMovilUrl && <source media="(max-width: 700px)" srcSet={promo.imagenMovilUrl} />}
      <img src={promo.imagenEscritorioUrl} alt={promo.titulo || 'Promoción FuturaTecno'} loading={active ? 'eager' : 'lazy'} />
    </picture>
    {(promo.titulo || promo.texto) && <div className="promotions-copy"><div>{promo.titulo && <h2>{promo.titulo}</h2>}{promo.texto && <p>{promo.texto}</p>}</div></div>}
  </>
}

export default function PromotionsCarousel() {
  const [items, setItems] = useState([])
  const [index, setIndex] = useState(0)
  const [paused, setPaused] = useState(false)
  const touchStart = useRef(null)

  useEffect(() => { axios.get('/api/promociones').then(r => setItems(r.data || [])).catch(() => {}) }, [])
  useEffect(() => {
    if (paused || items.length < 2) return undefined
    const timer = window.setInterval(() => setIndex(i => (i + 1) % items.length), 5000)
    return () => window.clearInterval(timer)
  }, [paused, items.length])
  useEffect(() => { if (index >= items.length) setIndex(0) }, [items.length, index])

  if (items.length < 4) return null
  const ir = next => setIndex((next + items.length) % items.length)
  const onPointerDown = e => { touchStart.current = e.clientX; setPaused(true) }
  const onPointerUp = e => {
    if (touchStart.current != null) {
      const delta = e.clientX - touchStart.current
      if (Math.abs(delta) > 45) ir(index + (delta < 0 ? 1 : -1))
    }
    touchStart.current = null; setPaused(false)
  }

  return <section className="promotions-carousel" aria-label="Promociones"
    onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)}
    onFocusCapture={() => setPaused(true)} onBlurCapture={() => setPaused(false)}
    onPointerDown={onPointerDown} onPointerUp={onPointerUp} onPointerCancel={() => { touchStart.current = null; setPaused(false) }}>
    <div className="promotions-track" style={{ transform: `translateX(-${index * 100}%)` }}>
      {items.map((promo, i) => <article className="promotions-slide" aria-hidden={i !== index} key={promo.id}>
        {promo.enlace ? (promo.enlace.startsWith('/')
          ? <Link to={promo.enlace} tabIndex={i === index ? 0 : -1}><SlideContent promo={promo} active={i === 0} /></Link>
          : <a href={promo.enlace} target="_blank" rel="noreferrer" tabIndex={i === index ? 0 : -1}><SlideContent promo={promo} active={i === 0} /></a>)
          : <SlideContent promo={promo} active={i === 0} />}
      </article>)}
    </div>
    <button className="promotions-arrow prev" type="button" aria-label="Promoción anterior" onClick={() => ir(index - 1)}>‹</button>
    <button className="promotions-arrow next" type="button" aria-label="Promoción siguiente" onClick={() => ir(index + 1)}>›</button>
    <div className="promotions-dots">{items.map((p, i) => <button type="button" key={p.id} className={i === index ? 'active' : ''} aria-label={`Ver promoción ${i + 1}`} aria-current={i === index} onClick={() => ir(i)} />)}</div>
  </section>
}
